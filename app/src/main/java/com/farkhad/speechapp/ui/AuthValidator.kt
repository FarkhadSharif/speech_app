package com.farkhad.speechapp.ui

data class AuthValidationResult(
    val emailError: String? = null,
    val passwordError: String? = null,
) {
    val isValid: Boolean
        get() = emailError == null && passwordError == null
}

object AuthValidator {
    fun validate(email: String, password: String, isSignUp: Boolean): AuthValidationResult {
        val emailError = validateEmail(email)
        val passwordError = when {
            password.isBlank() -> AuthMessages.PasswordRequired
            isSignUp && password.length < 8 -> AuthMessages.PasswordTooShort
            isSignUp && password.none(Char::isUpperCase) -> AuthMessages.PasswordNeedsUppercase
            isSignUp && password.none(Char::isLowerCase) -> AuthMessages.PasswordNeedsLowercase
            isSignUp && password.none(Char::isDigit) -> AuthMessages.PasswordNeedsDigit
            else -> null
        }
        return AuthValidationResult(emailError, passwordError)
    }

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> AuthMessages.EmailRequired
        !isValidEmail(email.trim()) -> AuthMessages.EmailInvalid
        else -> null
    }

    private fun isValidEmail(email: String): Boolean {
        if (email.any(Char::isWhitespace)) return false
        val atIndex = email.indexOf('@')
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) return false
        val domain = email.substring(atIndex + 1)
        return domain.length >= 3 &&
            domain.contains('.') &&
            !domain.startsWith('.') &&
            !domain.endsWith('.')
    }
}

object AuthMessages {
    const val EmailRequired = "Электрондық поштаны енгізіңіз / Введите электронную почту"
    const val EmailInvalid = "Электрондық пошта дұрыс емес / Некорректный email"
    const val PasswordRequired = "Құпия сөзді енгізіңіз / Введите пароль"
    const val PasswordTooShort = "Құпия сөз кемінде 8 таңба болуы керек / Пароль должен содержать минимум 8 символов"
    const val PasswordNeedsUppercase = "Бас әріпті қосыңыз / Добавьте заглавную букву"
    const val PasswordNeedsLowercase = "Кіші әріпті қосыңыз / Добавьте строчную букву"
    const val PasswordNeedsDigit = "Санды қосыңыз / Добавьте цифру"
    const val EmailAlreadyInUse = "Бұл email тіркелген. Кіруді таңдаңыз / Этот email уже зарегистрирован. Выполните вход"
    const val InvalidCredentials = "Email немесе құпия сөз қате / Неверный email или пароль"
    const val NetworkUnavailable = "Интернет байланысын тексеріңіз / Проверьте подключение к интернету"
    const val TooManyRequests = "Әрекет тым көп. Кейінірек қайталаңыз / Слишком много попыток. Повторите позже"
    const val VerificationSent = "Растау хаты жіберілді / Письмо для подтверждения отправлено"
    const val EmailStillUnverified = "Email әлі расталмаған. Хаттағы сілтемені ашыңыз / Email ещё не подтверждён. Откройте ссылку из письма"
    const val PasswordResetSent = "Егер аккаунт бар болса, қалпына келтіру хаты жіберілді / Если аккаунт существует, письмо для восстановления отправлено"
    const val NotAuthenticated = "Сессия аяқталды. Қайта кіріңіз / Сессия завершена. Войдите снова"
    const val ReauthenticationRequired = "Қауіпсіздік үшін құпия сөзді қайта енгізіңіз / Для безопасности повторно введите пароль"
    const val SessionsServiceUnavailable = "Барлық құрылғылардан шығу қызметі әлі қосылмаған немесе уақытша қолжетімсіз / Сервис выхода со всех устройств ещё не развёрнут или временно недоступен"
    const val SessionsRevoked = "Барлық құрылғылардағы сессиялар аяқталды / Сессии на всех устройствах завершены"
    const val UnexpectedError = "Күтпеген қате. Қайтадан көріңіз / Непредвиденная ошибка. Попробуйте ещё раз"
}
