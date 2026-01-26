package com.rasoiai.app.presentation.stats.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.CookingDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CookingCalendar(
    yearMonth: YearMonth,
    cookingDays: List<CookingDay>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = yearMonth == YearMonth.now()

    Column(modifier = modifier.fillMaxWidth()) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Text(
                    text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = onNextMonth,
                    enabled = !isCurrentMonth
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            TextButton(
                onClick = onTodayClick,
                enabled = !isCurrentMonth
            ) {
                Text("Today")
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val daysOfWeek = listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
            )

            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // Calendar grid
        CalendarGrid(
            yearMonth = yearMonth,
            cookingDays = cookingDays
        )
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    cookingDays: List<CookingDay>
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()

    // Calculate offset for first day (Sunday = 0)
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value % 7)

    // Create list of calendar cells
    val cells = buildList {
        // Add empty cells for offset
        repeat(firstDayOffset) { add(null) }

        // Add days of month
        var date = firstDayOfMonth
        while (!date.isAfter(lastDayOfMonth)) {
            val cookingDay = cookingDays.find { it.date == date }
            add(cookingDay ?: CookingDay(date = date, didCook = false))
            date = date.plusDays(1)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(((cells.size / 7 + 1) * 48).dp),
        userScrollEnabled = false
    ) {
        items(cells) { day ->
            CalendarDayCell(cookingDay = day)
        }
    }
}

@Composable
private fun CalendarDayCell(
    cookingDay: CookingDay?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (cookingDay == null) {
            // Empty cell
            return@Box
        }

        val isToday = cookingDay.isToday
        val didCook = cookingDay.didCook
        val isPast = cookingDay.isPast

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isToday) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    } else {
                        Modifier
                    }
                )
                .padding(4.dp)
        ) {
            // Day number
            Text(
                text = cookingDay.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.primary
                    cookingDay.isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Cooking indicator
            if (didCook) {
                Text(
                    text = "\uD83C\uDF73", // Cooking emoji
                    fontSize = 12.sp
                )
            } else if (isPast || isToday) {
                Text(
                    text = "\u25CB", // Empty circle
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CookingCalendarPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val yearMonth = YearMonth.now()
            val cookingDays = (1..yearMonth.lengthOfMonth()).map { day ->
                val date = yearMonth.atDay(day)
                CookingDay(
                    date = date,
                    didCook = date.dayOfMonth % 2 == 0 && !date.isAfter(LocalDate.now()),
                    mealsCount = if (date.dayOfMonth % 2 == 0) 2 else 0
                )
            }

            CookingCalendar(
                yearMonth = yearMonth,
                cookingDays = cookingDays,
                onPreviousMonth = {},
                onNextMonth = {},
                onTodayClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
