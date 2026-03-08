package com.rasoiai.app.presentation.achievements

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.LocalRasoiColors
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// region UI Models

/**
 * Wraps an Achievement with additional UI-level progress data.
 */
data class AchievementItem(
    val achievement: Achievement,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val category: AchievementCategory = AchievementCategory.COOKING
) {
    val progressFraction: Float
        get() = if (targetProgress > 0) {
            (currentProgress.toFloat() / targetProgress).coerceIn(0f, 1f)
        } else 0f

    val progressText: String
        get() = "$currentProgress/$targetProgress"
}

enum class AchievementCategory(val displayName: String) {
    COOKING("Cooking"),
    STREAKS("Streaks"),
    EXPLORATION("Exploration"),
    SOCIAL("Social"),
    MASTERY("Mastery")
}

// endregion

// region UiState

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val achievements: List<AchievementItem> = emptyList()
) {
    val unlockedAchievements: List<AchievementItem>
        get() = achievements.filter { it.achievement.isUnlocked }

    val lockedAchievements: List<AchievementItem>
        get() = achievements.filter { !it.achievement.isUnlocked }

    val totalCount: Int
        get() = achievements.size

    val unlockedCount: Int
        get() = unlockedAchievements.size

    val completionText: String
        get() = "$unlockedCount / $totalCount Unlocked"
}

// endregion

// region ViewModel

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            statsRepository.getAchievements().collect { achievements ->
                val enriched = enrichWithProgress(achievements)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        achievements = enriched
                    )
                }
            }
        }
    }

    /**
     * Enriches raw Achievement domain models with progress and category data.
     * In a production app, this would come from the repository/backend.
     * For now, we derive progress from known achievement IDs.
     */
    private fun enrichWithProgress(achievements: List<Achievement>): List<AchievementItem> {
        // Hardcoded progress mapping for sample data
        val progressMap = mapOf(
            "first-meal" to Triple(1, 1, AchievementCategory.COOKING),
            "7-day-streak" to Triple(7, 7, AchievementCategory.STREAKS),
            "master-chef" to Triple(25, 25, AchievementCategory.MASTERY),
            "50-meals" to Triple(50, 50, AchievementCategory.COOKING),
            "100-meals" to Triple(67, 100, AchievementCategory.COOKING),
            "30-day-streak" to Triple(12, 30, AchievementCategory.STREAKS)
        )

        // Additional achievements not in the repository (hardcoded for rich display)
        val extraAchievements = listOf(
            AchievementItem(
                achievement = Achievement(
                    id = "recipe-explorer",
                    name = "Recipe Explorer",
                    description = "Try recipes from all 4 cuisine regions",
                    emoji = "\uD83E\uDDED",
                    isUnlocked = true,
                    unlockedDate = LocalDate.now().minusDays(10)
                ),
                currentProgress = 4,
                targetProgress = 4,
                category = AchievementCategory.EXPLORATION
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "spice-master",
                    name = "Spice Master",
                    description = "Cook 10 dishes with high spice level",
                    emoji = "\uD83C\uDF36\uFE0F",
                    isUnlocked = false,
                    unlockedDate = null
                ),
                currentProgress = 6,
                targetProgress = 10,
                category = AchievementCategory.MASTERY
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "weekend-warrior",
                    name = "Weekend Warrior",
                    description = "Cook every weekend for a month",
                    emoji = "\uD83D\uDCAA",
                    isUnlocked = true,
                    unlockedDate = LocalDate.now().minusDays(5)
                ),
                currentProgress = 8,
                targetProgress = 8,
                category = AchievementCategory.STREAKS
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "pantry-pro",
                    name = "Pantry Pro",
                    description = "Add 20 items to your pantry",
                    emoji = "\uD83C\uDFE0",
                    isUnlocked = false,
                    unlockedDate = null
                ),
                currentProgress = 12,
                targetProgress = 20,
                category = AchievementCategory.COOKING
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "meal-plan-streak",
                    name = "Planner",
                    description = "Follow 3 complete weekly meal plans",
                    emoji = "\uD83D\uDCC5",
                    isUnlocked = false,
                    unlockedDate = null
                ),
                currentProgress = 1,
                targetProgress = 3,
                category = AchievementCategory.COOKING
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "festival-feast",
                    name = "Festival Feast",
                    description = "Cook a festival special recipe",
                    emoji = "\uD83C\uDF89",
                    isUnlocked = true,
                    unlockedDate = LocalDate.now().minusDays(20)
                ),
                currentProgress = 1,
                targetProgress = 1,
                category = AchievementCategory.EXPLORATION
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "social-chef",
                    name = "Social Chef",
                    description = "Share 5 achievements with friends",
                    emoji = "\uD83E\uDD1D",
                    isUnlocked = false,
                    unlockedDate = null
                ),
                currentProgress = 2,
                targetProgress = 5,
                category = AchievementCategory.SOCIAL
            ),
            AchievementItem(
                achievement = Achievement(
                    id = "breakfast-champion",
                    name = "Breakfast Champion",
                    description = "Cook breakfast for 14 consecutive days",
                    emoji = "\uD83C\uDF73",
                    isUnlocked = false,
                    unlockedDate = null
                ),
                currentProgress = 8,
                targetProgress = 14,
                category = AchievementCategory.STREAKS
            )
        )

        val fromRepo = achievements.map { achievement ->
            val (current, target, category) = progressMap[achievement.id]
                ?: Triple(
                    if (achievement.isUnlocked) 1 else 0,
                    1,
                    AchievementCategory.COOKING
                )
            AchievementItem(
                achievement = achievement,
                currentProgress = current,
                targetProgress = target,
                category = category
            )
        }

        // Merge: repo achievements first, then extras (avoid duplicates by id)
        val existingIds = fromRepo.map { it.achievement.id }.toSet()
        val extras = extraAchievements.filter { it.achievement.id !in existingIds }

        // Sort: unlocked first (newest first), then locked (highest progress first)
        return (fromRepo + extras).sortedWith(
            compareByDescending<AchievementItem> { it.achievement.isUnlocked }
                .thenByDescending { it.achievement.unlockedDate }
                .thenByDescending { it.progressFraction }
        )
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun buildShareText(item: AchievementItem): String {
        return buildString {
            appendLine("${item.achievement.emoji} ${item.achievement.name}")
            appendLine(item.achievement.description)
            if (item.achievement.isUnlocked) {
                appendLine("Unlocked on RasoiAI!")
            }
            append("#RasoiAI #CookingAchievements")
        }
    }
}

// endregion

// region Screen Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.ACHIEVEMENTS_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!uiState.isLoading) {
                            Text(
                                text = uiState.completionText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.ACHIEVEMENTS_LIST),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    // Unlocked Section
                    if (uiState.unlockedAchievements.isNotEmpty()) {
                        item {
                            Text(
                                text = "UNLOCKED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(
                                        start = spacing.md,
                                        end = spacing.md,
                                        top = spacing.md,
                                        bottom = spacing.xs
                                    )
                                    .testTag(TestTags.ACHIEVEMENTS_UNLOCKED_SECTION)
                            )
                        }

                        items(
                            items = uiState.unlockedAchievements,
                            key = { it.achievement.id }
                        ) { item ->
                            AchievementDetailCard(
                                item = item,
                                onShareClick = {
                                    val shareText = viewModel.buildShareText(item)
                                    val shareIntent = Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        },
                                        "Share Achievement"
                                    )
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    // Locked Section
                    if (uiState.lockedAchievements.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(spacing.sm))
                            Text(
                                text = "IN PROGRESS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(
                                        start = spacing.md,
                                        end = spacing.md,
                                        top = spacing.sm,
                                        bottom = spacing.xs
                                    )
                                    .testTag(TestTags.ACHIEVEMENTS_LOCKED_SECTION)
                            )
                        }

                        items(
                            items = uiState.lockedAchievements,
                            key = { it.achievement.id }
                        ) { item ->
                            AchievementDetailCard(
                                item = item,
                                onShareClick = null,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    // Bottom padding
                    item {
                        Spacer(modifier = Modifier.height(spacing.xl))
                    }
                }
            }
        }
    }
}

// endregion

// region Components

@Composable
private fun AchievementDetailCard(
    item: AchievementItem,
    onShareClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isUnlocked = item.achievement.isUnlocked
    val animatedProgress by animateFloatAsState(
        targetValue = item.progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )
    val rasoiColors = LocalRasoiColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${TestTags.ACHIEVEMENT_CARD_PREFIX}${item.achievement.id}"),
        shape = RoundedCornerShape(spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                rasoiColors.surfaceWarm
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji / Icon
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) item.achievement.emoji else "\uD83D\uDD12",
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Name
                Text(
                    text = item.achievement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xxs))

                // Description
                Text(
                    text = item.achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                if (isUnlocked) {
                    // Unlocked date
                    item.achievement.unlockedDate?.let { date ->
                        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                        Text(
                            text = "Unlocked ${date.format(formatter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Progress bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("${TestTags.ACHIEVEMENT_PROGRESS_PREFIX}${item.achievement.id}"),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        Text(
                            text = item.progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category badge
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }

            // Share button for unlocked achievements
            if (isUnlocked && onShareClick != null) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.testTag(
                        "${TestTags.ACHIEVEMENT_SHARE_PREFIX}${item.achievement.id}"
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share achievement",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// endregion

// region Previews

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun AchievementsScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val sampleItems = listOf(
                AchievementItem(
                    achievement = Achievement(
                        id = "first-meal",
                        name = "First Meal",
                        description = "Cook your first meal",
                        emoji = "\uD83C\uDFC5",
                        isUnlocked = true,
                        unlockedDate = LocalDate.now().minusDays(30)
                    ),
                    currentProgress = 1,
                    targetProgress = 1,
                    category = AchievementCategory.COOKING
                ),
                AchievementItem(
                    achievement = Achievement(
                        id = "7-day-streak",
                        name = "7-Day Streak",
                        description = "Cook for 7 consecutive days",
                        emoji = "\uD83E\uDD47",
                        isUnlocked = true,
                        unlockedDate = LocalDate.now().minusDays(15)
                    ),
                    currentProgress = 7,
                    targetProgress = 7,
                    category = AchievementCategory.STREAKS
                ),
                AchievementItem(
                    achievement = Achievement(
                        id = "100-meals",
                        name = "Century",
                        description = "Cook 100 total meals",
                        emoji = "\uD83D\uDCAF",
                        isUnlocked = false
                    ),
                    currentProgress = 67,
                    targetProgress = 100,
                    category = AchievementCategory.COOKING
                ),
                AchievementItem(
                    achievement = Achievement(
                        id = "spice-master",
                        name = "Spice Master",
                        description = "Cook 10 dishes with high spice level",
                        emoji = "\uD83C\uDF36\uFE0F",
                        isUnlocked = false
                    ),
                    currentProgress = 6,
                    targetProgress = 10,
                    category = AchievementCategory.MASTERY
                )
            )

            // Preview-only content (no ViewModel)
            AchievementsScreenPreviewContent(
                uiState = AchievementsUiState(
                    isLoading = false,
                    achievements = sampleItems
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementsScreenPreviewContent(uiState: AchievementsUiState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Achievements",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.completionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (uiState.unlockedAchievements.isNotEmpty()) {
                item {
                    Text(
                        text = "UNLOCKED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = spacing.md,
                            end = spacing.md,
                            top = spacing.md,
                            bottom = spacing.xs
                        )
                    )
                }
                items(uiState.unlockedAchievements, key = { it.achievement.id }) { item ->
                    AchievementDetailCard(
                        item = item,
                        onShareClick = {},
                        modifier = Modifier.padding(horizontal = spacing.md)
                    )
                }
            }
            if (uiState.lockedAchievements.isNotEmpty()) {
                item {
                    Text(
                        text = "IN PROGRESS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = spacing.md,
                            end = spacing.md,
                            top = spacing.md,
                            bottom = spacing.xs
                        )
                    )
                }
                items(uiState.lockedAchievements, key = { it.achievement.id }) { item ->
                    AchievementDetailCard(
                        item = item,
                        onShareClick = null,
                        modifier = Modifier.padding(horizontal = spacing.md)
                    )
                }
            }
        }
    }
}

// endregion
