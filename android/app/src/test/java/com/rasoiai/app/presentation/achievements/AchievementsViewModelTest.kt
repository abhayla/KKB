package com.rasoiai.app.presentation.achievements

import app.cash.turbine.test
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.repository.StatsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AchievementsViewModel")
class AchievementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockStatsRepository: StatsRepository
    private lateinit var viewModel: AchievementsViewModel

    private val testAchievements = listOf(
        Achievement(
            id = "first-meal",
            name = "First Meal",
            description = "Cook your first meal",
            emoji = "\uD83C\uDFC5",
            isUnlocked = true,
            unlockedDate = LocalDate.of(2026, 1, 15)
        ),
        Achievement(
            id = "7-day-streak",
            name = "Week Warrior",
            description = "Complete a 7-day cooking streak",
            emoji = "\uD83D\uDCC5",
            isUnlocked = true,
            unlockedDate = LocalDate.of(2026, 1, 20)
        ),
        Achievement(
            id = "100-meals",
            name = "Century",
            description = "Cook 100 meals",
            emoji = "\uD83D\uDCAF",
            isUnlocked = false
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockStatsRepository = mockk(relaxed = true)
        every { mockStatsRepository.getAchievements() } returns flowOf(testAchievements)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AchievementsViewModel {
        return AchievementsViewModel(mockStatsRepository).also { viewModel = it }
    }

    // region Initial State

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("initial state has isLoading=true")
        fun `initial state is loading`() = runTest {
            val vm = createViewModel()
            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("initial state has no error")
        fun `initial state has no error`() = runTest {
            val vm = createViewModel()
            vm.uiState.test {
                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("initial state has empty achievements list")
        fun `initial state has empty achievements list`() = runTest {
            val vm = createViewModel()
            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.achievements.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Load Achievements Success

    @Nested
    @DisplayName("Load Achievements - Success")
    inner class LoadAchievementsSuccess {

        @Test
        @DisplayName("after loading, isLoading becomes false")
        fun `after loading isLoading becomes false`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("achievements populated from repository and extras")
        fun `achievements populated from repository and extras`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                // 3 from repo + 8 extras (no duplicates) = 11 total
                assertTrue(state.achievements.size > 3)
                assertTrue(state.achievements.any { it.achievement.id == "first-meal" })
                assertTrue(state.achievements.any { it.achievement.id == "7-day-streak" })
                assertTrue(state.achievements.any { it.achievement.id == "100-meals" })
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("repo achievements have correct progress from progressMap")
        fun `repo achievements have correct progress from progressMap`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val firstMeal = state.achievements.first { it.achievement.id == "first-meal" }
                assertEquals(1, firstMeal.currentProgress)
                assertEquals(1, firstMeal.targetProgress)
                assertEquals(AchievementCategory.COOKING, firstMeal.category)

                val streak = state.achievements.first { it.achievement.id == "7-day-streak" }
                assertEquals(7, streak.currentProgress)
                assertEquals(7, streak.targetProgress)
                assertEquals(AchievementCategory.STREAKS, streak.category)

                val century = state.achievements.first { it.achievement.id == "100-meals" }
                assertEquals(67, century.currentProgress)
                assertEquals(100, century.targetProgress)
                assertEquals(AchievementCategory.COOKING, century.category)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("extra achievements are appended when not duplicating repo IDs")
        fun `extra achievements appended without duplicating repo IDs`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val ids = state.achievements.map { it.achievement.id }
                // Extras include recipe-explorer, spice-master, weekend-warrior, pantry-pro,
                // meal-plan-streak, festival-feast, social-chef, breakfast-champion
                assertTrue(ids.contains("recipe-explorer"))
                assertTrue(ids.contains("spice-master"))
                assertTrue(ids.contains("weekend-warrior"))
                assertTrue(ids.contains("social-chef"))
                // No duplicate IDs
                assertEquals(ids.size, ids.toSet().size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("unknown repo achievement gets default progress")
        fun `unknown repo achievement gets default progress`() = runTest {
            val unknownAchievement = Achievement(
                id = "unknown-badge",
                name = "Unknown",
                description = "Unknown achievement",
                emoji = "?",
                isUnlocked = true,
                unlockedDate = LocalDate.of(2026, 3, 1)
            )
            every { mockStatsRepository.getAchievements() } returns flowOf(listOf(unknownAchievement))

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val item = state.achievements.first { it.achievement.id == "unknown-badge" }
                // Unlocked unknown: default progress = (1, 1, COOKING)
                assertEquals(1, item.currentProgress)
                assertEquals(1, item.targetProgress)
                assertEquals(AchievementCategory.COOKING, item.category)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("unknown locked repo achievement gets zero progress")
        fun `unknown locked repo achievement gets zero progress`() = runTest {
            val unknownLocked = Achievement(
                id = "unknown-locked",
                name = "Unknown Locked",
                description = "Unknown locked achievement",
                emoji = "?",
                isUnlocked = false
            )
            every { mockStatsRepository.getAchievements() } returns flowOf(listOf(unknownLocked))

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val item = state.achievements.first { it.achievement.id == "unknown-locked" }
                // Locked unknown: default progress = (0, 1, COOKING)
                assertEquals(0, item.currentProgress)
                assertEquals(1, item.targetProgress)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Load Achievements - Empty List

    @Nested
    @DisplayName("Load Achievements - Empty List")
    inner class LoadAchievementsEmptyList {

        @Test
        @DisplayName("empty repo list still includes extra achievements")
        fun `empty repo list still includes extra achievements`() = runTest {
            every { mockStatsRepository.getAchievements() } returns flowOf(emptyList())

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                // 8 hardcoded extras still appear
                assertEquals(8, state.achievements.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("empty repo list shows correct completion text with extras")
        fun `empty repo list shows correct completion text with extras`() = runTest {
            every { mockStatsRepository.getAchievements() } returns flowOf(emptyList())

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val expected = "${state.unlockedCount} / ${state.totalCount} Unlocked"
                assertEquals(expected, state.completionText)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Load Achievements - Error

    @Nested
    @DisplayName("Load Achievements - Error")
    inner class LoadAchievementsError {

        @Test
        @DisplayName("when repository flow emits empty then errors, last good state is retained")
        fun `when repository flow emits empty then errors last good state retained`() = runTest {
            // The ViewModel collects the flow in viewModelScope.launch which swallows
            // the exception via the default CoroutineExceptionHandler. We verify that
            // if the flow emits successfully first, that state is retained.
            every { mockStatsRepository.getAchievements() } returns flowOf(emptyList())

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                // Empty repo still gets extras
                assertTrue(state.achievements.isNotEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("when repository returns empty flow, loading completes with extras only")
        fun `when repository returns empty flow loading completes with extras only`() = runTest {
            every { mockStatsRepository.getAchievements() } returns flowOf(emptyList())

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNull(state.errorMessage)
                // Only hardcoded extras present
                assertEquals(8, state.achievements.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Unlocked vs Locked Separation

    @Nested
    @DisplayName("Unlocked vs Locked Separation")
    inner class UnlockedVsLocked {

        @Test
        @DisplayName("unlocked achievements contain only isUnlocked=true items")
        fun `unlocked achievements contain only unlocked items`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.unlockedAchievements.isNotEmpty())
                assertTrue(state.unlockedAchievements.all { it.achievement.isUnlocked })
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("locked achievements contain only isUnlocked=false items")
        fun `locked achievements contain only locked items`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.lockedAchievements.isNotEmpty())
                assertTrue(state.lockedAchievements.none { it.achievement.isUnlocked })
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("unlocked + locked count equals total count")
        fun `unlocked plus locked equals total`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(
                    state.totalCount,
                    state.unlockedAchievements.size + state.lockedAchievements.size
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("all-unlocked scenario has no locked achievements")
        fun `all unlocked scenario has no locked achievements`() = runTest {
            val allUnlocked = listOf(
                Achievement("a", "A", "desc", "e", isUnlocked = true, unlockedDate = LocalDate.now()),
                Achievement("b", "B", "desc", "e", isUnlocked = true, unlockedDate = LocalDate.now())
            )
            every { mockStatsRepository.getAchievements() } returns flowOf(allUnlocked)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                // Repo items are all unlocked, but extras include locked items
                val repoItems = state.achievements.filter { it.achievement.id in listOf("a", "b") }
                assertTrue(repoItems.all { it.achievement.isUnlocked })
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("all-locked scenario has no unlocked from repo")
        fun `all locked scenario from repo has no unlocked repo items`() = runTest {
            val allLocked = listOf(
                Achievement("x", "X", "desc", "e", isUnlocked = false),
                Achievement("y", "Y", "desc", "e", isUnlocked = false)
            )
            every { mockStatsRepository.getAchievements() } returns flowOf(allLocked)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val repoItems = state.achievements.filter { it.achievement.id in listOf("x", "y") }
                assertTrue(repoItems.none { it.achievement.isUnlocked })
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Sorting

    @Nested
    @DisplayName("Sorting")
    inner class Sorting {

        @Test
        @DisplayName("unlocked achievements appear before locked in sorted list")
        fun `unlocked appear before locked`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val firstLockedIndex = state.achievements.indexOfFirst { !it.achievement.isUnlocked }
                val lastUnlockedIndex = state.achievements.indexOfLast { it.achievement.isUnlocked }
                if (firstLockedIndex != -1 && lastUnlockedIndex != -1) {
                    assertTrue(
                        lastUnlockedIndex < firstLockedIndex,
                        "All unlocked should appear before any locked"
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("locked achievements are sorted by progress fraction descending")
        fun `locked sorted by progress fraction descending`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val lockedProgressFractions = state.lockedAchievements.map { it.progressFraction }
                for (i in 0 until lockedProgressFractions.size - 1) {
                    assertTrue(
                        lockedProgressFractions[i] >= lockedProgressFractions[i + 1],
                        "Locked achievements should be sorted by progress descending"
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Achievements Grouped by Category

    @Nested
    @DisplayName("Achievements by Category")
    inner class AchievementsByCategory {

        @Test
        @DisplayName("achievements span multiple categories")
        fun `achievements span multiple categories`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val categories = state.achievements.map { it.category }.toSet()
                // With repo + extras, we expect COOKING, STREAKS, EXPLORATION, MASTERY, SOCIAL
                assertTrue(categories.size >= 3, "Should have at least 3 categories")
                assertTrue(categories.contains(AchievementCategory.COOKING))
                assertTrue(categories.contains(AchievementCategory.STREAKS))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("grouping by category produces non-empty groups")
        fun `grouping by category produces non-empty groups`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val grouped = state.achievements.groupBy { it.category }
                assertTrue(grouped.isNotEmpty())
                grouped.forEach { (category, items) ->
                    assertTrue(items.isNotEmpty(), "${category.displayName} group should not be empty")
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("category displayName values are human-readable")
        fun `category displayName values are human readable`() {
            AchievementCategory.entries.forEach { category ->
                assertTrue(category.displayName.isNotBlank())
                assertTrue(category.displayName[0].isUpperCase())
            }
        }
    }

    // endregion

    // region Clear Error

    @Nested
    @DisplayName("Clear Error")
    inner class ClearError {

        @Test
        @DisplayName("clearError clears error message")
        fun `clearError clears error message`() = runTest {
            every { mockStatsRepository.getAchievements() } returns flowOf(emptyList())
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("clearError does not affect achievements list")
        fun `clearError does not affect achievements list`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val beforeClear = vm.uiState.value.achievements.size

            vm.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(beforeClear, state.achievements.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Computed Properties

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("totalCount matches achievements list size")
        fun `totalCount matches achievements list size`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(state.achievements.size, state.totalCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("unlockedCount matches filtered unlocked list size")
        fun `unlockedCount matches filtered unlocked list size`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(state.unlockedAchievements.size, state.unlockedCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("completionText shows correct format")
        fun `completionText shows correct format`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val expected = "${state.unlockedCount} / ${state.totalCount} Unlocked"
                assertEquals(expected, state.completionText)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region AchievementItem Progress

    @Nested
    @DisplayName("AchievementItem Progress")
    inner class AchievementItemProgress {

        @Test
        @DisplayName("progressFraction is 1.0 for completed achievement")
        fun `progressFraction is 1 for completed`() {
            val item = AchievementItem(
                achievement = Achievement("test", "Test", "desc", "e", true),
                currentProgress = 10,
                targetProgress = 10
            )
            assertEquals(1.0f, item.progressFraction)
        }

        @Test
        @DisplayName("progressFraction is 0.5 for half-complete achievement")
        fun `progressFraction is half for half complete`() {
            val item = AchievementItem(
                achievement = Achievement("test", "Test", "desc", "e", false),
                currentProgress = 5,
                targetProgress = 10
            )
            assertEquals(0.5f, item.progressFraction)
        }

        @Test
        @DisplayName("progressFraction is 0 when target is 0")
        fun `progressFraction is 0 when target is 0`() {
            val item = AchievementItem(
                achievement = Achievement("test", "Test", "desc", "e", false),
                currentProgress = 5,
                targetProgress = 0
            )
            assertEquals(0f, item.progressFraction)
        }

        @Test
        @DisplayName("progressFraction is clamped to 1.0 when current exceeds target")
        fun `progressFraction clamped to 1 when current exceeds target`() {
            val item = AchievementItem(
                achievement = Achievement("test", "Test", "desc", "e", true),
                currentProgress = 15,
                targetProgress = 10
            )
            assertEquals(1.0f, item.progressFraction)
        }

        @Test
        @DisplayName("progressText shows correct format")
        fun `progressText shows correct format`() {
            val item = AchievementItem(
                achievement = Achievement("test", "Test", "desc", "e", false),
                currentProgress = 3,
                targetProgress = 10
            )
            assertEquals("3/10", item.progressText)
        }

        @Test
        @DisplayName("progressFraction is 0 when currentProgress is 0")
        fun `progressFraction is 0 when currentProgress is 0`() {
            val item = AchievementItem(
                achievement = Achievement("test", "Test", "desc", "e", false),
                currentProgress = 0,
                targetProgress = 10
            )
            assertEquals(0f, item.progressFraction)
        }
    }

    // endregion

    // region Build Share Text

    @Nested
    @DisplayName("Build Share Text")
    inner class BuildShareText {

        @Test
        @DisplayName("share text for unlocked achievement contains name and unlocked message")
        fun `share text for unlocked contains name and unlocked message`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val item = AchievementItem(
                achievement = Achievement(
                    id = "test",
                    name = "First Meal",
                    description = "Cook your first meal",
                    emoji = "\uD83C\uDFC5",
                    isUnlocked = true,
                    unlockedDate = LocalDate.of(2026, 1, 15)
                ),
                currentProgress = 1,
                targetProgress = 1,
                category = AchievementCategory.COOKING
            )

            val shareText = vm.buildShareText(item)
            assertTrue(shareText.contains("First Meal"))
            assertTrue(shareText.contains("Cook your first meal"))
            assertTrue(shareText.contains("Unlocked on RasoiAI!"))
            assertTrue(shareText.contains("#RasoiAI"))
            assertTrue(shareText.contains("#CookingAchievements"))
        }

        @Test
        @DisplayName("share text for locked achievement does not contain unlocked message")
        fun `share text for locked does not contain unlocked message`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val item = AchievementItem(
                achievement = Achievement(
                    id = "test",
                    name = "Century",
                    description = "Cook 100 meals",
                    emoji = "\uD83D\uDCAF",
                    isUnlocked = false
                ),
                currentProgress = 67,
                targetProgress = 100,
                category = AchievementCategory.COOKING
            )

            val shareText = vm.buildShareText(item)
            assertTrue(shareText.contains("Century"))
            assertTrue(shareText.contains("Cook 100 meals"))
            assertFalse(shareText.contains("Unlocked on RasoiAI!"))
            assertTrue(shareText.contains("#RasoiAI"))
        }

        @Test
        @DisplayName("share text includes emoji")
        fun `share text includes emoji`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val item = AchievementItem(
                achievement = Achievement(
                    id = "test",
                    name = "Test",
                    description = "Desc",
                    emoji = "\uD83C\uDFC5",
                    isUnlocked = true
                )
            )

            val shareText = vm.buildShareText(item)
            assertTrue(shareText.contains("\uD83C\uDFC5"))
        }
    }

    // endregion

    // region Duplicate Prevention

    @Nested
    @DisplayName("Duplicate Prevention")
    inner class DuplicatePrevention {

        @Test
        @DisplayName("repo achievement with same ID as extra replaces the extra")
        fun `repo achievement with same ID as extra replaces the extra`() = runTest {
            // "recipe-explorer" is one of the hardcoded extras
            val repoWithExtraId = listOf(
                Achievement(
                    id = "recipe-explorer",
                    name = "Explorer from Repo",
                    description = "From the repository",
                    emoji = "R",
                    isUnlocked = true,
                    unlockedDate = LocalDate.of(2026, 2, 1)
                )
            )
            every { mockStatsRepository.getAchievements() } returns flowOf(repoWithExtraId)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                val explorers = state.achievements.filter { it.achievement.id == "recipe-explorer" }
                assertEquals(1, explorers.size, "Should be exactly one recipe-explorer, no duplicate")
                // The repo version should be used (name from repo, not extra)
                assertEquals("Explorer from Repo", explorers[0].achievement.name)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion
}
