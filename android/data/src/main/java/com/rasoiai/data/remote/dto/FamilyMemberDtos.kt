package com.rasoiai.data.remote.dto

import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.SpecialDietaryNeed

/**
 * Family member response from backend API.
 */
data class FamilyMemberDto(
    val id: String,
    val name: String,
    val age_group: String?,
    val dietary_restrictions: List<String>?,
    val health_conditions: List<String>?
)

/**
 * Response wrapper for GET /api/v1/family-members.
 */
data class FamilyMembersListResponse(
    val members: List<FamilyMemberDto>,
    val total_count: Int
)

/**
 * Request body for POST /api/v1/family-members.
 */
data class FamilyMemberCreateRequest(
    val name: String,
    val age_group: String?,
    val dietary_restrictions: List<String>,
    val health_conditions: List<String>
)

/**
 * Request body for PUT /api/v1/family-members/{id}.
 */
data class FamilyMemberUpdateRequest(
    val name: String?,
    val age_group: String?,
    val dietary_restrictions: List<String>?,
    val health_conditions: List<String>?
)

/**
 * Map backend age_group string to domain MemberType.
 */
fun FamilyMemberDto.toDomain(): FamilyMember {
    return FamilyMember(
        id = id,
        name = name,
        type = when (age_group?.lowercase()) {
            "child", "teen" -> MemberType.CHILD
            "senior" -> MemberType.SENIOR
            else -> MemberType.ADULT
        },
        age = null,
        specialNeeds = (health_conditions.orEmpty()).mapNotNull { condition ->
            SpecialDietaryNeed.fromValue(condition)
        }
    )
}

/**
 * Map domain FamilyMember to backend create request.
 */
fun FamilyMember.toCreateRequest(): FamilyMemberCreateRequest {
    return FamilyMemberCreateRequest(
        name = name,
        age_group = type.value,
        dietary_restrictions = emptyList(),
        health_conditions = specialNeeds.map { it.value }
    )
}

/**
 * Map domain FamilyMember to backend update request.
 */
fun FamilyMember.toUpdateRequest(): FamilyMemberUpdateRequest {
    return FamilyMemberUpdateRequest(
        name = name,
        age_group = type.value,
        dietary_restrictions = emptyList(),
        health_conditions = specialNeeds.map { it.value }
    )
}
