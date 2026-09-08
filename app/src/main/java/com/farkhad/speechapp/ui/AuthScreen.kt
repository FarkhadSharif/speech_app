@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.data.AuthenticatedUser
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppNavy
import com.farkhad.speechapp.ui.theme.AppOutline
import com.farkhad.speechapp.ui.theme.AppRed
import com.farkhad.speechapp.ui.theme.AppSurfaceMuted
import com.farkhad.speechapp.ui.theme.AppText

@Composable
fun AuthLoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AuthBrandMark()
                Spacer(modifier = Modifier.height(28.dp))
                CircularProgressIndicator(color = AppRed, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Аккаунт тексерілуде\nПроверяем аккаунт",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppText.copy(alpha = 0.68f),
                )
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .padding(top = 22.dp, bottom = 22.dp)
                .background(AppRed),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 26.dp, end = 22.dp, top = 30.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            AuthBrandMark()

            Spacer(modifier = Modifier.height(42.dp))
            Text(
                text = "БАЛАҢЫЗБЕН БІРГЕ",
                color = AppRed,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = "Әр сөзге\nқуана қараймыз.",
                modifier = Modifier.padding(top = 8.dp),
                color = AppText,
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 41.sp,
                lineHeight = 44.sp,
            )
            Text(
                text = "Қазақша тыңдаймыз, қайталаймыз және сөйлесеміз. Сіз әр кішкентай жетістікті көріп отырасыз.",
                modifier = Modifier.padding(top = 14.dp),
                color = AppText.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
            )

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                color = AppNavy,
                shape = RoundedCornerShape(2.dp),
            ) {
                Text(
                    "Ә   Ғ   Қ   Ң   Ө   Ұ   Ү   І",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    color = Color(0xFFF3EFE3),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                )
            }

            Text(
                text = if (viewModel.isSignUpMode) "Отбасы үшін аккаунт ашу" else "Қош келдіңіз",
                modifier = Modifier.padding(top = 34.dp),
                color = AppText,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (viewModel.isSignUpMode) {
                    "Прогресті құрылғылар арасында сақтау үшін аккаунт жасаңыз."
                } else {
                    "Баланың жеке маршрутын жалғастырыңыз."
                },
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                color = AppText.copy(alpha = 0.60f),
                style = MaterialTheme.typography.bodySmall,
            )

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Электрондық пошта / Email") },
                isError = viewModel.emailError != null,
                supportingText = viewModel.emailError?.let { message -> { Text(message) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
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
                    Text("Құпия сөзді ұмыттыңыз ба? / Забыли пароль?", fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = viewModel::authenticate,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppNavy),
            ) {
                Text(
                    if (viewModel.isSignUpMode) "Тіркелу / Зарегистрироваться" else "Кіру / Войти",
                )
            }

            TextButton(onClick = viewModel::toggleAuthenticationMode) {
                Text(
                    text = if (viewModel.isSignUpMode) {
                        "Аккаунт бар ма? Кіру / Уже есть аккаунт? Войти"
                    } else {
                        "Аккаунт жоқ па? Тіркелу / Нет аккаунта? Регистрация"
                    },
                    textAlign = TextAlign.Start,
                )
            }

            androidx.compose.material3.Divider(
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                color = AppOutline,
            )
            Text(
                "Алдымен көріп шығуға болады",
                color = AppRed,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(
                onClick = viewModel::enterDemoMode,
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, AppText.copy(alpha = 0.45f)),
            ) {
                Text("Аккаунтсыз демо / Демо без аккаунта")
            }
            Text(
                text = "Деректер қорғалған. Бағалау медициналық диагноз болып саналмайды.",
                modifier = Modifier.padding(top = 18.dp),
                color = AppText.copy(alpha = 0.48f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (viewModel.isPasswordResetDialogVisible) {
        PasswordResetDialog(viewModel)
    }
}

@Composable
private fun AuthBrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(38.dp).background(AppRed))
        Column(modifier = Modifier.padding(start = 11.dp)) {
            Text(
                "SÓYLE",
                color = AppText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
            Text(
                "БАЛАМЕН БІРГЕ СӨЙЛЕСЕМІЗ",
                modifier = Modifier.padding(top = 1.dp),
                color = AppText.copy(alpha = 0.50f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
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
        shape = RoundedCornerShape(4.dp),
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
