package com.rasoiai.app.presentation.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Structural tests for TestTags. Per `.claude/rules/compose-testtags-convention.md`:
 * - All values must be unique (duplicates cause wrong-element selection in E2E)
 * - Values must be lowercase snake_case
 * - "*_PREFIX" constants must end with "_" so suffix concatenation is well-formed
 */
class TestTagsTest {

    /** Reflectively collect every `const val` on the [TestTags] object. */
    private fun allTags(): List<Pair<String, String>> {
        // TestTags is a Kotlin `object` — reflect declared fields, filter to String constants.
        return TestTags::class.java.declaredFields
            .filter { it.type == String::class.java }
            .map {
                it.isAccessible = true
                it.name to (it.get(null) as String)
            }
    }

    @Test
    @DisplayName("No duplicate tag values")
    fun `no duplicate tag values`() {
        val tags = allTags()
        val counts = tags.groupingBy { it.second }.eachCount()
        val dupes = counts.filter { it.value > 1 }
        assertTrue(
            dupes.isEmpty(),
            "Duplicate tag values would cause wrong-element selection in E2E tests: $dupes",
        )
    }

    @Test
    @DisplayName("No empty tag values")
    fun `no empty tag values`() {
        val empties = allTags().filter { it.second.isEmpty() }
        assertTrue(empties.isEmpty(), "Empty tag values are invalid: $empties")
    }

    @Test
    @DisplayName("All values use lowercase snake_case alphabet")
    fun `all values use lowercase snake_case alphabet`() {
        val pattern = Regex("^[a-z0-9_]+$")
        val offenders = allTags().filterNot { pattern.matches(it.second) }
        assertTrue(
            offenders.isEmpty(),
            "Tag values must be lowercase snake_case: $offenders",
        )
    }

    @Test
    @DisplayName("PREFIX constants end with trailing underscore")
    fun `prefix constants end with trailing underscore`() {
        val prefixOffenders = allTags()
            .filter { it.first.endsWith("_PREFIX") }
            .filterNot { it.second.endsWith("_") }
        assertTrue(
            prefixOffenders.isEmpty(),
            "*_PREFIX constants must end with '_' so suffix concatenation forms valid tags: $prefixOffenders",
        )
    }

    @Test
    @DisplayName("Non-prefix constants do NOT end with trailing underscore")
    fun `non-prefix constants do not end with trailing underscore`() {
        val offenders = allTags()
            .filterNot { it.first.endsWith("_PREFIX") }
            .filter { it.second.endsWith("_") }
        assertTrue(
            offenders.isEmpty(),
            "Non-PREFIX tags should not end with '_' (easy to mistake for a prefix): $offenders",
        )
    }

    @Test
    @DisplayName("TestTags is a singleton object (not a class)")
    fun `TestTags is a singleton object`() {
        // Verify via the kotlin reflection object instance — if it becomes a class,
        // callers would need to instantiate it and tag usage would break.
        val instance = TestTags::class.objectInstance
        assertTrue(instance != null, "TestTags must remain a Kotlin object")
    }

    @Test
    @DisplayName("All constants declared are reachable as Strings")
    fun `all constants declared are reachable as Strings`() {
        // Smoke test — at least 200 constants (sanity, current count is ~228).
        val count = allTags().size
        assertTrue(count >= 200, "Expected at least 200 tag constants, got $count")
    }
}
