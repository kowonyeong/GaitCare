package com.gaitcare.ui.measurement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MeasurementScreen(elderId: String, onFinish: () -> Unit) {
    val running = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("측정 화면 - 대상 ID: $elderId")
        Text(if (running.value) "측정 중..." else "대기 중")
        Button(onClick = { running.value = true }, modifier = Modifier.fillMaxWidth()) {
            Text("측정 시작")
        }
        Button(onClick = { running.value = false }, modifier = Modifier.fillMaxWidth()) {
            Text("측정 종료")
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("저장 후 돌아가기")
        }
    }
}
