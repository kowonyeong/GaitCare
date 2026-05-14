package com.gaitcare.ui.elder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gaitcare.data.FakeRepository

@Composable
fun ElderDetailScreen(elderId: String, onStartMeasurement: () -> Unit) {
    val records = FakeRepository.recordsOf(elderId)
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("이전 기록")
        LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
            items(records) { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = "${record.date} / ${record.durationSec}초 / ${record.summary}",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        Button(onClick = onStartMeasurement, modifier = Modifier.fillMaxWidth()) {
            Text("측정 시작")
        }
    }
}
