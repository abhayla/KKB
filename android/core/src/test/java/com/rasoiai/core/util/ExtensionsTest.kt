package com.rasoiai.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExtensionsTest {

    @Nested
    @DisplayName("safeCall")
    inner class SafeCall {
        @Test
        fun `returns Success when block succeeds`() = runTest {
            val result = safeCall { 42 }
            assertTrue(result.isSuccess)
            assertEquals(42, result.getOrNull())
        }

        @Test
        fun `returns Failure when block throws`() = runTest {
            val boom = IllegalStateException("boom")
            val result = safeCall<Int> { throw boom }
            assertTrue(result.isFailure)
            assertEquals(boom, result.exceptionOrNull())
        }

        @Test
        fun `rethrows CancellationException instead of wrapping`() = runTest {
            // Regression guard: without the CancellationException rethrow,
            // structured concurrency cancellation silently becomes Result.failure.
            try {
                safeCall<Int> { throw CancellationException("cancelled") }
                fail("Expected CancellationException to propagate, got Result wrapper")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }

        @Test
        fun `handles suspend calls`() = runTest {
            val result = safeCall {
                // Simulate a suspending operation returning a value.
                kotlinx.coroutines.delay(1)
                "done"
            }
            assertTrue(result.isSuccess)
            assertEquals("done", result.getOrNull())
        }
    }

    @Nested
    @DisplayName("Int.toReadableDuration")
    inner class ToReadableDuration {
        @Test
        fun `under an hour renders minutes`() {
            assertEquals("45m", 45.toReadableDuration())
            assertEquals("1m", 1.toReadableDuration())
            assertEquals("0m", 0.toReadableDuration())
            assertEquals("59m", 59.toReadableDuration())
        }

        @Test
        fun `exact hours render without minutes`() {
            assertEquals("1h", 60.toReadableDuration())
            assertEquals("2h", 120.toReadableDuration())
            assertEquals("5h", 300.toReadableDuration())
        }

        @Test
        fun `non-exact hours render hours and minutes`() {
            assertEquals("1h 30m", 90.toReadableDuration())
            assertEquals("2h 15m", 135.toReadableDuration())
            assertEquals("1h 1m", 61.toReadableDuration())
        }
    }

    @Nested
    @DisplayName("Int.toServingsText")
    inner class ToServingsText {
        @Test
        fun `single serving is singular`() {
            assertEquals("1 serving", 1.toServingsText())
        }

        @Test
        fun `plural servings`() {
            assertEquals("2 servings", 2.toServingsText())
            assertEquals("4 servings", 4.toServingsText())
            // Zero and negatives use plural too (match current impl).
            assertEquals("0 servings", 0.toServingsText())
        }
    }

    @Nested
    @DisplayName("String.toTitleCase")
    inner class ToTitleCase {
        @Test
        fun `capitalizes each word`() {
            assertEquals("Paneer Butter Masala", "paneer butter masala".toTitleCase())
        }

        @Test
        fun `lowercases non-first letters`() {
            assertEquals("Hello World", "HELLO WORLD".toTitleCase())
            assertEquals("Mixed Case Input", "mIxEd cAsE iNpUt".toTitleCase())
        }

        @Test
        fun `preserves single word`() {
            assertEquals("Dal", "DAL".toTitleCase())
        }

        @Test
        fun `handles empty string`() {
            assertEquals("", "".toTitleCase())
        }
    }

    @Nested
    @DisplayName("String.truncate")
    inner class Truncate {
        @Test
        fun `returns original when shorter than maxLength`() {
            assertEquals("hi", "hi".truncate(10))
        }

        @Test
        fun `returns original when equal to maxLength`() {
            assertEquals("hello", "hello".truncate(5))
        }

        @Test
        fun `truncates with ellipsis when longer`() {
            // "hello world" length 11, maxLength 8 -> "hello" + "..." = 8 chars
            assertEquals("hello...", "hello world".truncate(8))
        }

        @Test
        fun `ellipsis consumes 3 chars from budget`() {
            // maxLength 5 means we can show 2 chars + "..."
            val result = "abcdefgh".truncate(5)
            assertEquals("ab...", result)
            assertEquals(5, result.length)
        }
    }
}
