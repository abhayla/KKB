package com.rasoiai.domain.model

/**
 * Represents a user-created collection of favorite recipes.
 */
data class FavoriteCollection(
    val id: String,
    val name: String,
    val recipeIds: List<String>,
    val coverImageUrl: String?,
    val isDefault: Boolean = false,
    val createdAt: Long
) {
    val recipeCount: Int get() = recipeIds.size

    companion object {
        const val COLLECTION_ALL = "all"
        const val COLLECTION_RECENTLY_VIEWED = "recently-viewed"
    }
}

/**
 * Wraps a Recipe with additional favorite-specific metadata.
 */
data class FavoriteItem(
    val recipe: Recipe,
    val addedAt: Long,
    val collectionIds: List<String>
)
