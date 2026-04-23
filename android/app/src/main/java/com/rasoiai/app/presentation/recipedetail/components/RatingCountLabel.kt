package com.rasoiai.app.presentation.recipedetail.components

internal fun formatRatingCountLabel(count: Int): String =
    if (count == 1) "$count rating" else "$count ratings"
