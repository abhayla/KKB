package com.rasoiai.domain.model

import java.time.LocalDate

data class Festival(
    val id: String,
    val name: String,
    val date: LocalDate,
    val isFastingDay: Boolean,
    val suggestedDishes: List<String>
)
