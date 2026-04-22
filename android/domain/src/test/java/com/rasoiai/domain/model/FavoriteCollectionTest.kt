package com.rasoiai.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FavoriteCollectionTest {

    @Test
    @DisplayName("recipeCount reflects recipeIds.size")
    fun `recipeCount reflects recipeIds size`() {
        assertEquals(0, collection(recipeIds = emptyList()).recipeCount)
        assertEquals(1, collection(recipeIds = listOf("r-1")).recipeCount)
        assertEquals(3, collection(recipeIds = listOf("r-1", "r-2", "r-3")).recipeCount)
    }

    @Test
    @DisplayName("recipeCount does not deduplicate")
    fun `recipeCount does not deduplicate`() {
        // Contract: recipeCount is the list length, not the set size. Callers
        // that want dedup must apply it themselves.
        val c = collection(recipeIds = listOf("r-1", "r-1", "r-2"))
        assertEquals(3, c.recipeCount)
    }

    @Test
    @DisplayName("default isDefault is false")
    fun `default isDefault is false`() {
        assertEquals(false, collection().isDefault)
    }

    @Test
    @DisplayName("collection ID constants are stable")
    fun `collection ID constants are stable`() {
        // These strings are used as IDs in the DB and API — renaming them
        // silently breaks persisted state and cross-device sync.
        assertEquals("all", FavoriteCollection.COLLECTION_ALL)
        assertEquals("recently-viewed", FavoriteCollection.COLLECTION_RECENTLY_VIEWED)
    }

    @Test
    @DisplayName("collection ID constants are distinct")
    fun `collection ID constants are distinct`() {
        assertNotEquals(
            FavoriteCollection.COLLECTION_ALL,
            FavoriteCollection.COLLECTION_RECENTLY_VIEWED,
        )
    }

    private fun collection(
        id: String = "col-1",
        name: String = "My Favorites",
        recipeIds: List<String> = emptyList(),
        coverImageUrl: String? = null,
        isDefault: Boolean = false,
        createdAt: Long = 0L,
    ) = FavoriteCollection(
        id = id,
        name = name,
        recipeIds = recipeIds,
        coverImageUrl = coverImageUrl,
        isDefault = isDefault,
        createdAt = createdAt,
    )
}
