package com.example.alertapp.models.config

import kotlinx.serialization.Serializable

@Serializable
enum class FieldType {
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    DATE,
    TIME,
    DATETIME,
    URL,
    EMAIL,
    PHONE,
    CURRENCY,
    PERCENTAGE,
    JSON,
    LIST,
    CUSTOM
}

/**
 * Represents a validation rule for a configuration field.
 */
@Serializable
sealed class ValidationRule {
    @Serializable
    data class Required(
        val message: String = "This field is required"
    ) : ValidationRule()

    @Serializable
    data class MinLength(
        val length: Int,
        val message: String = "Minimum length is $length"
    ) : ValidationRule()

    @Serializable
    data class MaxLength(
        val length: Int,
        val message: String = "Maximum length is $length"
    ) : ValidationRule()

    @Serializable
    data class Pattern(
        val pattern: String, // Using String instead of Regex for serialization
        val message: String
    ) : ValidationRule()

    @Serializable
    data class Range(
        val min: Double,
        val max: Double,
        val message: String = "Value must be between $min and $max"
    ) : ValidationRule()

    @Serializable
    data class Custom(
        val id: String, // Identifier for the custom validation function
        val message: String
    ) : ValidationRule()
}

/**
 * Represents a configuration field with type, validation rules, and metadata.
 */
@Serializable
data class ConfigField(
    val key: String,
    val label: String,
    val type: FieldType,
    val defaultValue: String? = null, // Using String for cross-platform compatibility
    val required: Boolean = false,
    val validations: List<ValidationRule> = emptyList(),
    val description: String? = null,
    val placeholder: String? = null,
    val options: List<String>? = null, // Using String for cross-platform compatibility
    val dependsOn: String? = null,
    val visible: Boolean = true,
    val enabled: Boolean = true,
    val metadata: Map<String, String>? = null // Using String for cross-platform compatibility
) {
    /**
     * Validates a value against this field's validation rules.
     * Returns list of validation error messages, empty if validation passes.
     */
    fun validate(value: String?): List<String> {
        val errors = mutableListOf<String>()

        // Check required field
        if (required && value == null) {
            errors.add((validations.find { it is ValidationRule.Required } as? ValidationRule.Required)?.message
                ?: "This field is required")
            return errors
        }

        // Skip other validations if value is null and field is not required
        if (value == null) return errors

        // Apply all validation rules
        validations.forEach { rule ->
            when (rule) {
                is ValidationRule.Required -> {
                    // Already handled above
                }
                is ValidationRule.MinLength -> {
                    if (value.length < rule.length) {
                        errors.add(rule.message)
                    }
                }
                is ValidationRule.MaxLength -> {
                    if (value.length > rule.length) {
                        errors.add(rule.message)
                    }
                }
                is ValidationRule.Pattern -> {
                    if (!Regex(rule.pattern).matches(value)) {
                        errors.add(rule.message)
                    }
                }
                is ValidationRule.Range -> {
                    val numValue = value.toDoubleOrNull()
                    if (numValue != null && (numValue < rule.min || numValue > rule.max)) {
                        errors.add(rule.message)
                    }
                }
                is ValidationRule.Custom -> {
                    // Custom validations must be implemented platform-specifically
                    // through the id field
                }
            }
        }

        return errors
    }
}
