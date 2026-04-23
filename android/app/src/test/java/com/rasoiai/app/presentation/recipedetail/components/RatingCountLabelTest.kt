package com.rasoiai.app.presentation.recipedetail.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RatingCountLabelTest {

    @Test
    fun `singular form when count is 1`() {
        assertEquals("1 rating", formatRatingCountLabel(1))
    }

    @Test
    fun `plural form when count is zero`() {
        assertEquals("0 ratings", formatRatingCountLabel(0))
    }

    @Test
    fun `plural form when count is greater than 1`() {
        assertEquals("2 ratings", formatRatingCountLabel(2))
        assertEquals("100 ratings", formatRatingCountLabel(100))
    }
}
