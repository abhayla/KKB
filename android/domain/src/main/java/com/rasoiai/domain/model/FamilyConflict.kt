package com.rasoiai.domain.model

/**
 * Detail about a single family safety conflict.
 */
data class ConflictDetail(
    val memberName: String,
    val condition: String,
    val keyword: String,
    val ruleTarget: String
)

/**
 * Exception thrown when an INCLUDE rule conflicts with family member health conditions.
 * Contains structured conflict details for UI display.
 */
class FamilyConflictException(
    message: String,
    val conflictDetails: List<ConflictDetail>
) : Exception(message)
