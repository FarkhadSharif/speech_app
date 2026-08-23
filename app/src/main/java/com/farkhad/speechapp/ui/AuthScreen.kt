package com.farkhad.speechapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.checkAuthStatus()
    }

    LaunchedEffect(viewModel.isAuthenticated) {
        if (viewModel.isAuthenticated) {
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (viewModel.isSignUpMode) "Тіркелу" else "Қайта оралуыңызбен!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("Электрондық пошта") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("Құпия сөз") },
            singleLine = true,
            visualTransformation = if (viewModel.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { viewModel.isPasswordVisible = !viewModel.isPasswordVisible }) {
                    Text(if (viewModel.isPasswordVisible) "Жасыру" else "Көрсету")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.isSignUpMode) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.childName,
                onValueChange = { viewModel.childName = it },
                label = { Text("Баланың есімі") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.childAge,
                onValueChange = { viewModel.childAge = it },
                label = { Text("Баланың жасы") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.parentPin,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) viewModel.parentPin = it },
                label = { Text("Ата-ана PIN-коды (4 сан)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        viewModel.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.authenticate() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isSignUpMode) "Тіркелу" else "Кіру")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    viewModel.isSignUpMode = !viewModel.isSignUpMode
                    viewModel.errorMessage = null
                }
            ) {
                Text(
                    if (viewModel.isSignUpMode)
                        "Аккаунтыңыз бар ма? Кіру"
                    else
                        "Аккаунтыңыз жоқ па? Тіркелу"
                )
            }
        }
    }
}