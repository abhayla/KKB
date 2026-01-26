package com.rasoiai.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents an item in the user's pantry with expiry tracking.
 */
data class PantryItem(
    val id: String,
    val name: String,
    val category: PantryCategory,
    val quantity: Int = 1,
    val unit: String = "piece",
    val addedDate: LocalDate,
    val expiryDate: LocalDate?,
    val imageUrl: String? = null
) {
    /**
     * Days remaining until expiry. Null if no expiry (grains, spices).
     */
    val daysUntilExpiry: Int?
        get() = expiryDate?.let {
            ChronoUnit.DAYS.between(LocalDate.now(), it).toInt()
        }

    /**
     * Whether the item is expiring soon (within 2 days).
     */
    val isExpiringSoon: Boolean
        get() = daysUntilExpiry?.let { it in 0..2 } ?: false

    /**
     * Whether the item has expired.
     */
    val isExpired: Boolean
        get() = daysUntilExpiry?.let { it < 0 } ?: false

    /**
     * Display text for expiry (e.g., "3d", "Expired", "No expiry").
     */
    val expiryDisplayText: String
        get() = when {
            expiryDate == null -> "No expiry"
            isExpired -> "Expired"
            else -> "${daysUntilExpiry}d"
        }
}

/**
 * Categories for pantry items with default shelf life.
 */
enum class PantryCategory(
    val displayName: String,
    val emoji: String,
    val defaultShelfLifeDays: Int?
) {
    LEAFY_VEGETABLES("Leafy Vegetables", "🥬", 3),
    VEGETABLES("Vegetables", "🥔", 7),
    FRUITS("Fruits", "🍎", 5),
    DAIRY_MILK("Dairy (Milk, Curd)", "🥛", 5),
    DAIRY_PANEER("Dairy (Paneer, Cheese)", "🧀", 7),
    EGGS("Eggs", "🥚", 14),
    BREAD("Bread", "🍞", 5),
    GRAINS("Grains & Pulses", "🌾", null),
    SPICES("Spices", "🌶️", null),
    MEAT("Meat & Poultry", "🍖", 3),
    SEAFOOD("Seafood", "🐟", 2),
    OTHER("Other", "📦", 7);

    companion object {
        fun fromDisplayName(name: String): PantryCategory =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
    }
}

/**
 * Result of scanning pantry items from an image.
 */
data class PantryScanResult(
    val detectedItems: List<DetectedItem>,
    val confidence: Float
)

/**
 * An item detected from a pantry scan.
 */
data class DetectedItem(
    val name: String,
    val suggestedCategory: PantryCategory,
    val quantity: Int = 1
)
