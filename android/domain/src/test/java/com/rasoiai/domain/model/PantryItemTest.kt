package com.rasoiai.domain.model

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PantryItemTest {

    @Nested
    @DisplayName("daysUntilExpiry")
    inner class DaysUntilExpiry {
        @Test
        fun `null when no expiry date (grains spices)`() {
            assertNull(pantryItem(expiryDate = null).daysUntilExpiry)
        }

        @Test
        fun `positive for future expiry`() {
            val item = pantryItem(expiryDate = LocalDate.now().plusDays(5))
            assertEquals(5, item.daysUntilExpiry)
        }

        @Test
        fun `zero on the expiry date itself`() {
            assertEquals(0, pantryItem(expiryDate = LocalDate.now()).daysUntilExpiry)
        }

        @Test
        fun `negative after expiry`() {
            val item = pantryItem(expiryDate = LocalDate.now().minusDays(3))
            assertEquals(-3, item.daysUntilExpiry)
        }
    }

    @Nested
    @DisplayName("isExpiringSoon")
    inner class IsExpiringSoon {
        @Test
        fun `false when no expiry`() {
            assertFalse(pantryItem(expiryDate = null).isExpiringSoon)
        }

        @Test
        fun `true when 0, 1, or 2 days from now`() {
            assertTrue(pantryItem(expiryDate = LocalDate.now()).isExpiringSoon)
            assertTrue(pantryItem(expiryDate = LocalDate.now().plusDays(1)).isExpiringSoon)
            assertTrue(pantryItem(expiryDate = LocalDate.now().plusDays(2)).isExpiringSoon)
        }

        @Test
        fun `false when 3 or more days away`() {
            assertFalse(pantryItem(expiryDate = LocalDate.now().plusDays(3)).isExpiringSoon)
            assertFalse(pantryItem(expiryDate = LocalDate.now().plusDays(7)).isExpiringSoon)
        }

        @Test
        fun `false when already expired`() {
            assertFalse(pantryItem(expiryDate = LocalDate.now().minusDays(1)).isExpiringSoon)
        }
    }

    @Nested
    @DisplayName("isExpired")
    inner class IsExpired {
        @Test
        fun `false when no expiry`() {
            assertFalse(pantryItem(expiryDate = null).isExpired)
        }

        @Test
        fun `false on the expiry date (edge - still usable today)`() {
            assertFalse(pantryItem(expiryDate = LocalDate.now()).isExpired)
        }

        @Test
        fun `true day after expiry`() {
            assertTrue(pantryItem(expiryDate = LocalDate.now().minusDays(1)).isExpired)
            assertTrue(pantryItem(expiryDate = LocalDate.now().minusDays(30)).isExpired)
        }
    }

    @Nested
    @DisplayName("expiryDisplayText")
    inner class ExpiryDisplayText {
        @Test
        fun `No expiry when expiryDate null`() {
            assertEquals("No expiry", pantryItem(expiryDate = null).expiryDisplayText)
        }

        @Test
        fun `Expired when past`() {
            assertEquals("Expired", pantryItem(expiryDate = LocalDate.now().minusDays(1)).expiryDisplayText)
        }

        @Test
        fun `Nd format when days remaining`() {
            assertEquals("0d", pantryItem(expiryDate = LocalDate.now()).expiryDisplayText)
            assertEquals("5d", pantryItem(expiryDate = LocalDate.now().plusDays(5)).expiryDisplayText)
        }
    }

    @Nested
    @DisplayName("PantryCategory")
    inner class PantryCategoryTests {
        @Test
        fun `fromDisplayName matches case-insensitively`() {
            assertEquals(PantryCategory.VEGETABLES, PantryCategory.fromDisplayName("Vegetables"))
            assertEquals(PantryCategory.VEGETABLES, PantryCategory.fromDisplayName("vegetables"))
            assertEquals(PantryCategory.VEGETABLES, PantryCategory.fromDisplayName("VEGETABLES"))
        }

        @Test
        fun `fromDisplayName unknown falls back to OTHER`() {
            assertEquals(PantryCategory.OTHER, PantryCategory.fromDisplayName("Magic Dust"))
        }

        @Test
        fun `GRAINS and SPICES have null default shelf life (long-lasting)`() {
            assertNull(PantryCategory.GRAINS.defaultShelfLifeDays)
            assertNull(PantryCategory.SPICES.defaultShelfLifeDays)
        }

        @Test
        fun `perishable categories have positive shelf life`() {
            assertTrue(PantryCategory.DAIRY_MILK.defaultShelfLifeDays!! > 0)
            assertTrue(PantryCategory.MEAT.defaultShelfLifeDays!! > 0)
            assertTrue(PantryCategory.SEAFOOD.defaultShelfLifeDays!! > 0)
        }

        @Test
        fun `seafood has shortest shelf life among perishables (sanity)`() {
            // Seafood spoils fastest — confirms the map doesn't accidentally
            // invert this relationship after future edits.
            val seafood = PantryCategory.SEAFOOD.defaultShelfLifeDays!!
            val meat = PantryCategory.MEAT.defaultShelfLifeDays!!
            assertTrue(seafood <= meat, "Seafood should not outlast meat")
        }
    }

    private fun pantryItem(
        expiryDate: LocalDate?,
    ) = PantryItem(
        id = "p-1",
        name = "Test",
        category = PantryCategory.OTHER,
        addedDate = LocalDate.now(),
        expiryDate = expiryDate,
    )
}
