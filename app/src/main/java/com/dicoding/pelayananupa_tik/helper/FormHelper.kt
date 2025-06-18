package com.dicoding.pelayananupa_tik.helper

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

data class ValidationRule(
    val field: String,
    val layout: TextInputLayout?,
    val errorMessage: String,
    val validationType: ValidationType
)

enum class ValidationType {
    REQUIRED,
    PHONE,
    RADIO_BUTTON,
    FILE
}

class ValidationBuilder {
    private val rules = mutableListOf<ValidationRule>()

    fun required(field: String, layout: TextInputLayout, errorMessage: String) {
        rules.add(ValidationRule(field, layout, errorMessage, ValidationType.REQUIRED))
    }

    fun phone(field: String, layout: TextInputLayout, errorMessage: String = "Format nomor telepon tidak valid") {
        rules.add(ValidationRule(field, layout, errorMessage, ValidationType.PHONE))
    }

    fun radioButton(field: String, errorMessage: String) {
        rules.add(ValidationRule(field, null, errorMessage, ValidationType.RADIO_BUTTON))
    }

    fun file(uri: Uri?, context: Context, errorMessage: String = "Harap pilih file") {
        rules.add(ValidationRule(uri?.toString() ?: "", null, errorMessage, ValidationType.FILE))
    }

    fun build(): List<ValidationRule> = rules
}

fun buildValidation(block: ValidationBuilder.() -> Unit): List<ValidationRule> {
    return ValidationBuilder().apply(block).build()
}

object ValidationHelper {

    fun validateFormWithRules(context: Context, rules: List<ValidationRule>): ValidationResult {
        val errors = mutableListOf<String>()
        var isValid = true

        for (rule in rules) {
            val fieldValid = when (rule.validationType) {
                ValidationType.REQUIRED -> {
                    validateRequiredField(rule.field, rule.layout, rule.errorMessage)
                }
                ValidationType.PHONE -> {
                    validatePhoneField(rule.field, rule.layout, "Kontak tidak boleh kosong", rule.errorMessage)
                }
                ValidationType.RADIO_BUTTON -> {
                    validateRadioButtonField(rule.field, rule.errorMessage, context)
                }
                ValidationType.FILE -> {
                    validateFileField(rule.field, rule.errorMessage, context)
                }
            }

            if (!fieldValid) {
                isValid = false
                errors.add(rule.errorMessage)
            }
        }

        return ValidationResult(isValid, errors)
    }
}

fun isValidPhoneNumber(phoneNumber: String): Boolean {
    val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
    if (digitsOnly.length < 10 || digitsOnly.length > 15) {
        return false
    }
    return when {
        digitsOnly.startsWith("62") -> {
            val localNumber = digitsOnly.substring(2)
            isValidIndonesianLocalNumber(localNumber)
        }
        digitsOnly.startsWith("0") -> {
            val localNumber = digitsOnly.substring(1)
            isValidIndonesianLocalNumber(localNumber)
        }
        else -> {
            isValidIndonesianLocalNumber(digitsOnly)
        }
    }
}

fun isValidIndonesianLocalNumber(localNumber: String): Boolean {
    if (localNumber.length < 9 || localNumber.length > 13) {
        return false
    }
    val validPrefixes = listOf(
        // Telkomsel
        "811", "812", "813", "821", "822", "823", "851", "852", "853",
        // Indosat
        "814", "815", "816", "855", "856", "857", "858",
        // XL
        "817", "818", "819", "859", "877", "878",
        // Tri (3)
        "895", "896", "897", "898", "899",
        // Smartfren
        "881", "882", "883", "884", "885", "886", "887", "888", "889",
        // Axis
        "831", "832", "833", "838",
        // Telkom (PSTN)
        "21", "22", "24", "31", "341", "343", "361", "370", "380", "401", "411", "421", "431", "451", "471", "481", "511", "541", "561", "571", "601", "620", "651", "717", "721", "741", "751", "761", "771", "778"
    )
    return validPrefixes.any { prefix -> localNumber.startsWith(prefix) }
}

fun validateRequiredField(field: String, layout: TextInputLayout?, errorMsg: String): Boolean {
    return if (field.isBlank()) {
        layout?.error = errorMsg
        false
    } else {
        layout?.error = null
        true
    }
}

fun validatePhoneField(
    phone: String,
    layout: TextInputLayout?,
    emptyError: String = "Kontak tidak boleh kosong",
    formatError: String = "Format nomor telepon tidak valid"
): Boolean {
    return when {
        phone.isBlank() -> {
            layout?.error = emptyError
            false
        }
        !isValidPhoneNumber(phone) -> {
            layout?.error = formatError
            false
        }
        else -> {
            layout?.error = null
            true
        }
    }
}

fun validateRadioButtonField(
    selectedValue: String,
    errorMessage: String,
    context: Context
): Boolean {
    return if (selectedValue.isEmpty()) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        false
    } else {
        true
    }
}

fun validateFileField(
    fileUri: String,
    errorMessage: String,
    context: Context
): Boolean {
    return if (fileUri.isEmpty()) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        false
    } else {
        true
    }
}