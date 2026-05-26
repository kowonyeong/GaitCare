# GaitCare 보행 평가 알고리즘 상세 문서

본 문서는 GaitCare의 보행 신호 처리 흐름과 점수 계산 방식을 자세히 설명합니다.
모든 알고리즘은 [app/src/main/java/com/inhatc/gaitcare/sensor/GaitAnalyzer.kt](../app/src/main/java/com/inhatc/gaitcare/sensor/GaitAnalyzer.kt) 와 [GaitSensorManager.kt](../app/src/main/java/com/inhatc/gaitcare/sensor/GaitSensorManager.kt) 에 구현되어 있습니다.

---

## 1. 신호 수집

### 센서
| 센서 | 타입 | 샘플링 속도 | 용도 |
|------|------|------------|------|
| 가속도계 | `TYPE_ACCELEROMETER` | ≈50Hz (`SAMPLE_RATE_US = 20_000µs`) | 보행 진동·중력 |
| 자이로스코프 | `TYPE_GYROSCOPE` | ≈50Hz | 회전 안정성 |

### 측정 단계

```
IDLE ───[측정 시작 버튼]──► CALIBRATING (10초) ───► MEASURING ───[종료 버튼]──► FINISHED
```

- **CALIBRATING (10초)**: 사용자가 걷는 동안 축 자동 보정
- **MEASURING**: 본 측정 (최소 20초, 권장 30초 이상)

각 샘플은 `[ax, ay, az, gx, gy, gz]` 형태로 저장되며, 타임스탬프는 Long 별도 리스트로 보관 (Float 변환 시 정밀도 손실 방지).

---

## 2. 축 자동 캘리브레이션

사용자가 휴대폰을 어떤 방향으로 주머니에 넣든 자동으로 사람 기준 축을 식별합니다.

### 원리
- **수직축 (Vertical)**: 중력 방향. 평균 절댓값이 가장 큰 축
- **전방축 (Forward)**: 보행 진동이 가장 큰 축 (분산 최대)
- **좌우축 (Lateral)**: 나머지 한 축

### 코드 ([GaitAnalyzer.kt#calibrateAxes](../app/src/main/java/com/inhatc/gaitcare/sensor/GaitAnalyzer.kt))

```kotlin
fun calibrateAxes() {
    if (rawSamples.size < CALIBRATION_SAMPLES) return

    val calibSamples = rawSamples.take(CALIBRATION_SAMPLES)

    // ① 전방축 = 분산 최대
    val varX = variance(calibSamples.map { it[0] })
    val varY = variance(calibSamples.map { it[1] })
    val varZ = variance(calibSamples.map { it[2] })
    forwardAxis = argmaxOf(varX, varY, varZ)

    // ② 수직축 = 평균 절댓값 최대
    val meanAbsX = calibSamples.map { abs(it[0]) }.average().toFloat()
    val meanAbsY = calibSamples.map { abs(it[1]) }.average().toFloat()
    val meanAbsZ = calibSamples.map { abs(it[2]) }.average().toFloat()
    verticalAxis = argmaxOf(meanAbsX, meanAbsY, meanAbsZ)

    // ③ 좌우축 = 나머지
    lateralAxis = (0..2).first { it != forwardAxis && it != verticalAxis }
}
```

### 통계적 직관
| 축 | 분산 | 평균 절댓값 |
|----|------|------------|
| 수직 | 작음 (중력 ≈ 일정 9.8) | **큼** (≈9.5 m/s²) |
| 전방 | **큼** (걸음마다 ±2~3 m/s² 진동) | 작음 (≈2 m/s²) |
| 좌우 | 작음 | 작음 (≈0.5 m/s²) |

---

## 3. 신호 처리

### 중력 제거 (저주파 필터)
지수가중 이동평균(`α=0.15`)으로 저주파 성분(중력) 추정 → 원신호에서 빼서 동적 가속도만 추출.

```kotlin
private fun lowPassFilter(signal: List<Float>, alpha: Float): List<Float> {
    var prev = signal.firstOrNull() ?: 0f
    return signal.map { v -> alpha * v + (1 - alpha) * prev .also { prev = it } }
}

val dynForward  = rawForward  - lowPassFilter(rawForward,  α)
val dynLateral  = rawLateral  - lowPassFilter(rawLateral,  α)
val dynVertical = rawVertical - lowPassFilter(rawVertical, α)
```

`dynForward`, `dynLateral`, `dynVertical` = 중력 성분이 제거된 순수 보행 진동 신호.

---

## 4. 걸음 검출

리듬 / 대칭성 계산에 필요하므로 내부적으로만 사용 (UI에는 표시되지 않음).

### 알고리즘
3축 동적 가속도의 **합성 크기**에서 피크 검출.

```kotlin
val dynMagnitudes = indices.map { i ->
    sqrt(dynForward[i]² + dynLateral[i]² + dynVertical[i]²)
}
```

피크 조건:
1. 이동평균 평활(window ≈ sampleRate/6)
2. 평균 대비 임계 초과: `v > min(mean × 1.03, mean + 0.08)`
3. 좌우 이웃보다 큰 값
4. 이전 피크와 220ms 이상 간격

피크가 4개 미만이면 임계를 더 낮춰(`mean × 1.005`) 재검출하여 셔플링 보행에서도 검출되도록 보장.

### 검출 결과
`steps: List<Long>` — 각 걸음 발생 시각의 timestamp(ms) 리스트.

---

## 5. 다섯 가지 보행 지표

### 5.1 흔들림 (Shakiness) — 가중치 25%

**무엇을 측정?** 보행 중 좌우·상하 진동의 크기. 클수록 불안정.

**수식**
```
combined = (lateralRms + verticalRms × 0.5) / g
```
- `lateralRms`, `verticalRms` = 동적 가속도의 RMS
- 좌우(lateralRms)에 더 큰 가중치 (좌우 흔들림이 낙상 위험에 더 직결)
- 중력 단위(g)로 정규화

**점수 변환**
| combined (g) | 점수 |
|--------------|------|
| < 0.08 | 95 |
| < 0.15 | 88 |
| < 0.22 | 80 |
| < 0.32 | 72 |
| < 0.45 | 63 |
| < 0.62 | 53 |
| < 0.85 | 43 |
| ≥ 0.85 | 33 |

---

### 5.2 보행 리듬 (Rhythm) — 가중치 25%

**무엇을 측정?** 걸음 간격이 얼마나 일정한지. 변동계수 CV(%) 사용.

**수식**
```
stepIntervals = [steps[i+1] - steps[i] for i in 0..n-2]
CV = std(stepIntervals) / mean(stepIntervals)
CV(%) = CV × 100
```

220~3000ms 범위 밖 간격은 노이즈로 간주해 제외.

**점수 변환**
| CV (%) | 점수 |
|--------|------|
| < 4 | 95 |
| < 8 | 88 |
| < 14 | 80 |
| < 22 | 72 |
| < 32 | 63 |
| < 45 | 53 |
| < 60 | 43 |
| ≥ 60 | 33 |

---

### 5.3 보행 대칭성 (Symmetry) — 가중치 20%

**무엇을 측정?** 짝수번째와 홀수번째 걸음의 추진력 차이.

**수식**
```
stepMags[i] = |dynForward at steps[i]|
oddMags  = [stepMags[i] for i even]   ← 한쪽 발 그룹
evenMags = [stepMags[i] for i odd]    ← 반대쪽 발 그룹

SI = |mean(oddMags) - mean(evenMags)| / ((mean(odd) + mean(even)) / 2)
SI(%) = SI × 100
```

**⚠️ 한계**: 휴대폰이 한쪽 주머니에 있어 그쪽 발 디딤에서 더 큰 진동이 잡힙니다. 건강한 사람도 baseline 비대칭이 존재하므로, **개인 내 시간 추세 추적**에 적합하고 **개인 간 비교는 부적합**합니다.

**점수 변환**
| SI (%) | 점수 |
|--------|------|
| < 3 | 95 |
| < 6 | 88 |
| < 12 | 80 |
| < 20 | 72 |
| < 30 | 63 |
| < 45 | 53 |
| < 60 | 43 |
| ≥ 60 | 33 |

---

### 5.4 회전 안정성 (Rotation) — 가중치 20%

**무엇을 측정?** 자이로 합성 각속도의 평균. 방향 전환 시 흔들림을 반영.

**수식**
```
gyroMag[i] = √(gx[i]² + gy[i]² + gz[i]²)
avgRotation = mean(gyroMag)   [rad/s]
avgRotation(°/s) = avgRotation × 180/π
```

**점수 변환**
| 각속도 (°/s) | 점수 |
|-------------|------|
| < 15 | 95 |
| < 32 | 88 |
| < 52 | 80 |
| < 78 | 72 |
| < 108 | 63 |
| < 145 | 53 |
| < 185 | 43 |
| ≥ 185 | 33 |

---

### 5.5 보행 속도 (Speed) — 가중치 10%

**무엇을 측정?** 전방축 동적 가속도의 RMS. 걸음 수가 아닌 진폭으로 보행의 추진력 추정.

**왜 걸음 수가 아닌가?**
- 노인의 셔플링 보행은 가속도 피크가 약해 걸음 검출 신뢰도가 낮음
- 검출 실패 시 분당 걸음수(cadence)가 0이 되어 부당하게 낮은 점수
- 전방축 RMS는 검출 임계와 무관, 모든 종류의 보행에서 안정적

**수식**
```
forwardRms = sqrt(mean(dynForward²))
forwardRmsG = forwardRms / g
```

**점수 변환**
| forwardRmsG (g) | 점수 | 의미 |
|----------------|------|------|
| < 0.04 | 30 | 거의 정지 |
| < 0.08 | 55 | 매우 느린 셔플 |
| < 0.13 | 73 | 느린 노인 보행 |
| < 0.25 | 90 | 정상 노인 보행 |
| < 0.45 | 88 | 활발한 보행 |
| < 0.75 | 73 | 매우 빠른 보행 |
| ≥ 0.75 | 55 | 비정상 (조깅/충격) |

---

## 6. 종합 점수

```
totalScore = round(
    shakinessScore × 0.25 +
    rhythmScore    × 0.25 +
    symmetryScore  × 0.20 +
    rotationScore  × 0.20 +
    cadenceScore   × 0.10
)
totalScore = clamp(totalScore, 0, 100)
```

### 등급
| 점수 | 등급 |
|------|------|
| 80~100 | 매우 좋음 |
| 65~79 | 좋음 |
| 50~64 | 보통 |
| 35~49 | 주의 필요 |
| <35 | 위험 |

---

## 7. 시그널 처리 파이프라인 요약

```
원시 가속도 (ax,ay,az)
     │
     ├─ [축 캘리브레이션] ──► forwardAxis / verticalAxis / lateralAxis
     │
     ├─ [중력 제거 LPF α=0.15]
     │
     ▼
동적 가속도 (dynForward, dynLateral, dynVertical)
     │
     ├──► 합성 크기 ──► [피크 검출] ──► steps
     │                                    │
     │                                    ├──► step intervals ──► CV ──► rhythmScore
     │                                    │
     │                                    └──► 피크 magnitudes ──► SI ──► symmetryScore
     │
     ├──► lateralRms, verticalRms ──► combined ──► shakinessScore
     │
     └──► forwardRms ──► forwardRmsG ──► cadenceScore

자이로 (gx,gy,gz) ──► magnitude ──► avgRotation ──► rotationScore

shakiness × 0.25 + rhythm × 0.25 + symmetry × 0.20 + rotation × 0.20 + speed × 0.10
                                  │
                                  ▼
                            총점 (0~100)
```

---

## 8. 핵심 상수

| 상수 | 값 | 의미 |
|------|----|----|
| `SAMPLE_RATE_US` | 20_000 | 가속도/자이로 샘플링 간격 (50Hz) |
| `CALIBRATION_DURATION_MS` | 10_000 | 캘리브레이션 시간 |
| `CALIBRATION_SAMPLES` | 50 | 분석 시작 전 워밍업 |
| `LOW_PASS_ALPHA` | 0.15 | LPF 가중치 (중력 추정) |
| `STEP_MIN_INTERVAL_MS` | 220 | 걸음 사이 최소 간격 |
| `STEP_MAX_INTERVAL_MS` | 3000 | 걸음 사이 최대 간격 (셔플 인정) |
| `STEP_THRESHOLD_MULTIPLIER` | 1.03 | 피크 검출 평균 대비 임계 |
| `STEP_THRESHOLD_ABS` | 0.08 m/s² | 피크 검출 절대 임계 |
| `GRAVITY` | 9.80665 | 중력 가속도 |
| **가중치** | 25/25/20/20/10 | 흔들림/리듬/대칭/회전/속도 |

---

## 9. 알려진 제약

- **임상 검증 미실시**: 학술 평가 기준과의 상관관계 검증 필요
- **소규모 표본**: 점수 경계값은 일반적인 보행 문헌과 추정에 기반
- **대칭성 baseline 편향**: 휴대폰 위치 의존성 (위 5.3 참조)
- **걸음 검출 신뢰도**: 셔플링 보행에서 낮음 (RMS 기반 속도로 보완)
- **하드웨어 차이**: 단말기 모델별 센서 노이즈 특성 차이 미보정
