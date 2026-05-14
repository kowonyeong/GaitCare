# GaitCare

요양원 기관에서 어르신 보행 평가를 수행하기 위한 Android 앱 프로젝트입니다.

## 기능 범위 (MVP)
- 로그인 / 회원가입
- 기관 메인 화면에서 어르신 목록 조회 및 선택
- 어르신 상세 화면
  - 이전 측정 기록 목록
  - 측정 시작 버튼
- 측정 화면
  - 측정 진행 상태 표시
  - 측정 종료 및 결과 저장

## 기술 스택
- Android Studio (최신 안정 버전 권장)
- Kotlin
- Jetpack Compose + Material3
- Navigation Compose
- ViewModel + StateFlow

## 프로젝트 구조
```
app/
  src/main/java/com/gaitcare/
    data/               # 모델 및 저장소
    ui/
      auth/             # 로그인/회원가입
      facility/         # 기관 메인(어르신 목록)
      elder/            # 어르신 상세/기록
      measurement/      # 측정
      navigation/       # 네비게이션 그래프
      theme/            # 디자인 시스템
```

## 시작 방법
1. Android Studio에서 루트 폴더를 열기
2. Gradle Sync 실행
3. `app` 모듈 실행

## 다음 단계 제안
- Room 연동으로 로컬 기록 저장
- 실제 인증 API 연동
- BLE/센서 연동으로 보행 데이터 수집
