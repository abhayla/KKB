package com.rasoiai.domain.model

/**
 * Thrown when attempting to create a recipe rule that duplicates an existing rule.
 *
 * @param message Human-readable message describing the duplicate
 * @param existingRuleId The ID of the existing duplicate rule
 */
class DuplicateRuleException(
    message: String,
    val existingRuleId: String
) : Exception(message)
