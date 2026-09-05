@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.data.AuthenticatedUser

@Composable
fun AuthLoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Аккаунт тексерілуде\nПроверяем аккаунт",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (viewModel.isSignUpMode) {
                    "Ата-ана аккаунтын құру"
                } else {
                    "Қайта оралуыңызбен!"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (viewModel.isSignUpMode) {
                    "Регистрация родительского аккаунта"
                } else {
                    "Войдите в родительский аккаунт"
                },
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Электрондық пошта / Email") },
                isError = viewModel.emailError != null,
                supportingText = viewModel.emailError?.let { message -> { Text(message) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(10.dp))

            PasswordField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChanged,
                isVisible = viewModel.isPasswordVisible,
                onToggleVisibility = viewModel::togglePasswordVisibility,
                error = viewModel.passwordError,
            )

            viewModel.errorMessage
                ?.takeIf { viewModel.emailError == null && viewModel.passwordError == null }
                ?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    MessageText(message, isError = true)
                }

            viewModel.authNoticeMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                MessageText(message, isError = false)
            }

            if (!viewModel.isSignUpMode) {
                TextButton(
                    onClick = viewModel::openPasswordResetDialog,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Құпия сөзді ұмыттыңыз ба? / Забыли пароль?")
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = viewModel::authenticate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (viewModel.isSignUpMode) {
                        "Тіркелу / Зарегистрироваться"
                    } else {
                        "Кіру / Войти"
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = viewModel::toggleAuthenticationMode) {
                Text(
                    text = if (viewModel.isSignUpMode) {
                        "Аккаунт бар ма? Кіру / Уже есть аккаунт? Войти"
                    } else {
                        "Аккаунт жоқ па? Тіркелу / Нет аккаунта? Регистрация"
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (viewModel.isPasswordResetDialogVisible) {
        PasswordResetDialog(viewModel)
    }
}

@Composable
fun EmailVerificationScreen(
    viewModel: AuthViewModel,
    user: AuthenticatedUser,
) {
    val isLoading = viewModel.verificationActionState is AccountActionUiState.Loading
    val cooldown = viewModel.verificationCooldownSeconds
    val resendLabel = if (cooldown > 0) {
        "Қайта жіберу: ${cooldown}с / Повторно: ${cooldown}с"
    } else {
        "Хатты қайта жіберу / Отправить письмо повторно"
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✉️", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Email-ды растаңыз\nПодтвердите email",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Растау сілтемесі жіберілді. Хаттағы сілтемені ашып, осы экранға оралыңыз.\n\nМы отправили ссылку для подтверждения. Откройте её в письме и вернитесь сюда.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            user.email?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }

            when (val state = viewModel.verificationActionState) {
                AccountActionUiState.Idle -> Unit
                AccountActionUiState.Loading -> {
                    Spacer(modifier = Modifier.height(20.dp))
                    CircularProgressIndicator()
                }
                is AccountActionUiState.Success -> {
                    Spacer(modifier = Modifier.height(18.dp))
                    MessageText(state.message, isError = false)
                }
                is AccountActionUiState.Error -> {
                    Spacer(modifier = Modifier.height(18.dp))
                    MessageText(state.message, isError = true)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = viewModel::refreshEmailVerification,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Email-ды растадым / Я подтвердил email")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = viewModel::resendVerificationEmail,
                enabled = !isLoading && cooldown == 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(resendLabel, textAlign = TextAlign.Center)
            }
            TextButton(
                onClick = viewModel::signOut,
                enabled = !isLoading,
            ) {
                Text("Басқа аккаунтпен кіру / Войти в другой аккаунт")
            }
        }
    }
}

@Composable
private fun PasswordResetDialog(viewModel: AuthViewModel) {
    val isLoading = viewModel.passwordResetState is AccountActionUiState.Loading
    AlertDialog(
        onDismissRequest = viewModel::closePasswordResetDialog,
        title = { Text("Құпия сөзді қалпына келтіру\nВосстановление пароля") },
        text = {
            Column {
                Text("Email-ды енгізіңіз. Нәтиже аккаунттың бар-жоғын көрсетпейді.\nВведите email. Результат не раскрывает наличие аккаунта.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.passwordResetEmail,
                    onValueChange = viewModel::onPasswordResetEmailChanged,
                    label = { Text("Email") },
                    isError = viewModel.passwordResetEmailError != null,
                    supportingText = viewModel.passwordResetEmailError?.let { message ->
                        { Text(message) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (val state = viewModel.passwordResetState) {
                    AccountActionUiState.Idle -> Unit
                    AccountActionUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    is AccountActionUiState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        MessageText(state.message, isError = false)
                    }
                    is AccountActionUiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        MessageText(state.message, isError = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::sendPasswordResetEmail,
                enabled = !isLoading,
            ) {
                Text("Жіберу / Отправить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = viewModel::closePasswordResetDialog,
                enabled = !isLoading,
            ) {
                Text("Жабу / Закрыть")
            }
        },
    )
}

@Composable
fun SignOutEverywhereDialog(viewModel: AuthViewModel) {
    val isLoading = viewModel.signOutEverywhereState is AccountActionUiState.Loading
    AlertDialog(
        onDismissRequest = viewModel::closeSignOutEverywhereDialog,
        title = { Text("Барлық құрылғылардан шығу\nВыйти на всех устройствах") },
        text = {
            Column {
                Text("Қауіпсіздік үшін ағымдағы құпия сөзді қайта енгізіңіз.\nДля безопасности повторно введите текущий пароль.")
                Spacer(modifier = Modifier.height(12.dp))
                PasswordField(
                    value = viewModel.reauthenticationPassword,
                    onValueChange = viewModel::onReauthenticationPasswordChanged,
                    isVisible = viewModel.isReauthenticationPasswordVisible,
                    onToggleVisibility = viewModel::toggleReauthenticationPasswordVisibility,
                    enabled = !isLoading,
                )
                when (val state = viewModel.signOutEverywhereState) {
                    AccountActionUiState.Idle -> Unit
                    AccountActionUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AccountActionUiState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        MessageText(state.message, isError = false)
                    }
                    is AccountActionUiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        MessageText(state.message, isError = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::signOutEverywhere,
                enabled = !isLoading,
            ) {
                Text("Барлығынан шығу / Выйти везде")
            }
        },
        dismissButton = {
            TextButton(
                onClick = viewModel::closeSignOutEverywhereDialog,
                enabled = !isLoading,
            ) {
                Text("Бас тарту / Отмена")
            }
        },
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    error: String? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Құпия сөз / Пароль") },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(onClick = onToggleVisibility, enabled = enabled) {
                Text(
                    text = if (isVisible) "Жасыру" else "Көрсету",
                    fontSize = 12.sp,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MessageText(message: String, isError: Boolean) {
    Text(
        text = message,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}
