package com.protectednet.utilizr

import androidx.core.util.PatternsCompat
import com.protectednet.utilizr.GetText.L
import kotlin.reflect.KClass

data class ValidationResult(
    val isValid: Boolean,
    val message: String? = null
)

fun interface Validator {
    fun validate(input: String?): ValidationResult
}

object EmailValidator : Validator {

    override fun validate(input: String?): ValidationResult {
        if (input.isNullOrBlank()) {
            return ValidationResult(false, L.t("Please enter an email address"))
        }

        if (!PatternsCompat.EMAIL_ADDRESS.matcher(input).matches()) {
            return ValidationResult(false, L.t("Please enter a valid email address"))
        }

        return ValidationResult(true)
    }

}

open class MinLengthValidator(
    private val minLength: Int,
    private val zeroLengthMessage: String,
    private val lessThanMessage: String
) : Validator {

    override fun validate(input: String?): ValidationResult {
        return when {
            input.isNullOrBlank() -> ValidationResult(false, zeroLengthMessage)
            input.length < minLength -> ValidationResult(false, lessThanMessage)
            else -> ValidationResult(true)
        }
    }

}

object PasswordValidator : Validator {
    override fun validate(input: String?): ValidationResult {
        return when  {
            input.isNullOrBlank() -> ValidationResult(false, L.t("Please enter a password"))
            else -> ValidationResult(true)
        }
    }
}

class NumberValidator<T> @PublishedApi internal constructor(
    private val type: KClass<T>,
    private val minimum: T,
    private val maximum: T
) : Validator where T : Number, T : Comparable<T> {

    companion object {
        inline operator fun <reified T> invoke(minimum: T, maximum: T): NumberValidator<T>
                where T : Number, T : Comparable<T> = NumberValidator(T::class, minimum, maximum)
    }

    override fun validate(input: String?): ValidationResult {
        if (input.isNullOrBlank()) {
            return ValidationResult(false, L.t("Please enter a number."))
        }

        val parsed = when (type) {
            Int::class -> input.toIntOrNull() as T?
            Double::class -> input.toDoubleOrNull() as T?
            Float::class -> input.toFloatOrNull() as T?
            Long::class -> input.toLongOrNull() as T?
            Short::class -> input.toShortOrNull() as T?
            Byte::class -> input.toByteOrNull() as T?
            else -> throw IllegalStateException()
        } ?: return ValidationResult(false, L.t("Please enter digits only."))

        if (parsed < minimum) {
            return ValidationResult(
                false,
                L.t("Please enter a number equal to or greater than {0}", minimum)
            )
        }

        if (parsed > maximum) {
            return ValidationResult(
                false,
                L.t("Please enter a number equal to or less than {0}", maximum)
            )
        }

        return ValidationResult(true)
    }

}
