package com.gaitcare.ui.facility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gaitcare.data.FakeRepository

@Composable
fun FacilityHomeScreen(onElderClick: (String) -> Unit) {
    val elders = FakeRepository.elders
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("어르신 선택")
        LazyColumn {
            items(elders) { elder ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onElderClick(elder.id) }) {
                    Text("${elder.name} / ${elder.age}세 / ${elder.roomNumber}호", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
