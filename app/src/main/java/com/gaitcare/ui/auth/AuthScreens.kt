package com.gaitcare.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onSignUpClick: () -> Unit) {
    val id = remember { mutableStateOf("") }
    val pw = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GaitCare 로그인")
        OutlinedTextField(value = id.value, onValueChange = { id.value = it }, label = { Text("아이디") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pw.value, onValueChange = { pw.value = it }, label = { Text("비밀번호") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onLoginSuccess, modifier = Modifier.fillMaxWidth()) { Text("로그인") }
        Button(onClick = onSignUpClick, modifier = Modifier.fillMaxWidth()) { Text("회원가입") }
    }
}

@Composable
fun SignUpScreen(onSignUpComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("회원가입")
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("기관명") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("담당자") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("이메일") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSignUpComplete, modifier = Modifier.fillMaxWidth()) { Text("가입 완료") }
    }
}
