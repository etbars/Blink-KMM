package com.example.alertapp.validation

import kotlinx.serialization.Serializable

/**
 * Represents the result of a validation operation.
 */
@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap(),
    val warnings: Map<String, String> = emptyMap()
) {
    /**
     * Returns true if there are any validation errors.
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Returns true if there are any validation warnings.
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Returns all validation messages (errors and warnings) as a formatted string.
     */
    fun getFormattedMessages(): String = buildString {
        if (hasErrors()) {
            appendLine("Errors:")
            errors.forEach { (field, message) ->
                appendLine("- $field: $message")
            }
        }
        if (hasWarnings()) {
            if (hasErrors()) appendLine()
            appendLine("Warnings:")
            warnings.forEach { (field, message) ->
                appendLine("- $field: $message")
            }
        }
    }

    companion object {
        /**
         * Creates a successful validation result with no errors or warnings.
         */
        fun success() = ValidationResult(isValid = true)

        /**
         * Creates a failed validation result with the specified errors.
         */
        fun failure(vararg errors: Pair<String, String>) = ValidationResult(
            isValid = false,
            errors = errors.toMap()
        )

        /**
         * Creates a validation result with warnings but no errors.
         */
        fun warning(vararg warnings: Pair<String, String>) = ValidationResult(
            isValid = true,
            warnings = warnings.toMap()
        )

        /**
         * Combines multiple validation results into a single result.
         */
        fun combine(vararg results: ValidationResult): ValidationResult {
            val combinedErrors = mutableMapOf<String, String>()
            val combinedWarnings = mutableMapOf<String, String>()

            results.forEach { result ->
                combinedErrors.putAll(result.errors)
                combinedWarnings.putAll(result.warnings)
            }

            return ValidationResult(
                isValid = combinedErrors.isEmpty(),
                errors = combinedErrors,
                warnings = combinedWarnings
            )
        }

        /**
         * Creates a validation result from a list of field validations.
         */
        fun fromFieldValidations(validations: List<FieldValidation>): ValidationResult {
            val errors = mutableMapOf<String, String>()
            val warnings = mutableMapOf<String, String>()

            validations.forEach { validation ->
                when (validation) {
                    is FieldValidation.Error -> errors[validation.field] = validation.message
                    is FieldValidation.Warning -> warnings[validation.field] = validation.message
                }
            }

            return ValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
                warnings = warnings
            )
        }
    }
}

/**
 * Represents a validation result for a specific field.
 */
@Serializable
sealed class FieldValidation {
    abstract val field: String
    abstract val message: String

    @Serializable
    data class Error(
        override val field: String,
        override val message: String
    ) : FieldValidation()

    @Serializable
    data class Warning(
        override val field: String,
        override val message: String
    ) : FieldValidation()
}
