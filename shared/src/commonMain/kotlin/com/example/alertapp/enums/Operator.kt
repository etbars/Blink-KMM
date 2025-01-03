package com.example.alertapp.enums

import kotlinx.serialization.Serializable

/**
 * Represents comparison operators for weather conditions.
 */
@Serializable
enum class Operator {
    GREATER_THAN,
    LESS_THAN,
    EQUAL_TO,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    NOT_EQUAL_TO;

    /**
     * Evaluates the operator with the given values.
     * @param value1 First value to compare
     * @param value2 Second value to compare
     * @return Result of the comparison
     */
    fun evaluate(value1: Double, value2: Double): Boolean = when (this) {
        GREATER_THAN -> value1 > value2
        LESS_THAN -> value1 < value2
        EQUAL_TO -> value1 == value2
        GREATER_THAN_OR_EQUAL -> value1 >= value2
        LESS_THAN_OR_EQUAL -> value1 <= value2
        NOT_EQUAL_TO -> value1 != value2
    }

    /**
     * Returns the string representation of the operator.
     */
    override fun toString(): String = when (this) {
        GREATER_THAN -> ">"
        LESS_THAN -> "<"
        EQUAL_TO -> "="
        GREATER_THAN_OR_EQUAL -> ">="
        LESS_THAN_OR_EQUAL -> "<="
        NOT_EQUAL_TO -> "!="
    }
}
