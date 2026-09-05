package com.farkhad.speechapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun validRegistrationCredentialsPass() {
        val result = AuthValidator.validate("parent@example.kz", "StrongPass1", isSignUp = true)

        assertTrue(result.isValid)
        assertNull(result.emailError)
        assertNull(result.passwordError)
    }

    @Test
    fun blankCredentialsReturnBilingualErrors() {
        val result = AuthValidator.validate("", "", isSignUp = false)

        assertFalse(result.isValid)
        assertEquals(AuthMessages.EmailRequired, result.emailError)
        assertEquals(AuthMessages.PasswordRequired, result.passwordError)
        assertTrue(result.emailError!!.contains("/"))
        assertTrue(result.passwordError!!.contains("/"))
    }

    @Test
    fun malformedEmailIsRejected() {
        val invalidEmails = listOf("parent", "@example.kz", "parent@example", "parent @example.kz")

        invalidEmails.forEach { email ->
            assertEquals(
                "Expected $email to be rejected",
                AuthMessages.EmailInvalid,
                AuthValidator.validate(email, "StrongPass1", isSignUp = true).emailError,
            )
        }
    }

    @Test
    fun registrationPasswordRequiresLengthCaseAndDigit() {
        assertEquals(
            AuthMessages.PasswordTooShort,
            AuthValidator.validate("a@b.kz", "Aa1", isSignUp = true).passwordError,
        )
        assertEquals(
            AuthMessages.PasswordNeedsUppercase,
            AuthValidator.validate("a@b.kz", "lowercase1", isSignUp = true).passwordError,
        )
        assertEquals(
            AuthMessages.PasswordNeedsLowercase,
            AuthValidator.validate("a@b.kz", "UPPERCASE1", isSignUp = true).passwordError,
        )
        assertEquals(
            AuthMessages.PasswordNeedsDigit,
            AuthValidator.validate("a@b.kz", "NoDigitsHere", isSignUp = true).passwordError,
        )
    }

    @Test
    fun loginAllowsExistingAccountWithLegacyShortPassword() {
        val result = AuthValidator.validate("parent@example.kz", "old", isSignUp = false)

        assertTrue(result.isValid)
    }
}
