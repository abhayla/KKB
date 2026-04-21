package com.rasoiai.app.presentation.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ResourceTest {

    @Nested
    @DisplayName("State predicates")
    inner class StatePredicates {
        @Test
        fun `Loading has only isLoading true`() {
            val r = Resource.Loading
            assertTrue(r.isLoading)
            assertFalse(r.isSuccess)
            assertFalse(r.isError)
        }

        @Test
        fun `Success has only isSuccess true`() {
            val r: Resource<Int> = Resource.Success(42)
            assertFalse(r.isLoading)
            assertTrue(r.isSuccess)
            assertFalse(r.isError)
        }

        @Test
        fun `Error has only isError true`() {
            val r: Resource<Int> = Resource.Error("oops")
            assertFalse(r.isLoading)
            assertFalse(r.isSuccess)
            assertTrue(r.isError)
        }
    }

    @Nested
    @DisplayName("Accessors")
    inner class Accessors {
        @Test
        fun `getOrNull returns data for Success`() {
            assertEquals("hi", Resource.Success("hi").getOrNull())
        }

        @Test
        fun `getOrNull returns null for Error`() {
            assertNull(Resource.Error("oops").getOrNull())
        }

        @Test
        fun `getOrNull returns null for Loading`() {
            assertNull(Resource.Loading.getOrNull())
        }

        @Test
        fun `errorOrNull returns message for Error`() {
            assertEquals("boom", Resource.Error("boom").errorOrNull())
        }

        @Test
        fun `errorOrNull returns null for Success`() {
            assertNull(Resource.Success(1).errorOrNull())
        }

        @Test
        fun `errorOrNull returns null for Loading`() {
            assertNull(Resource.Loading.errorOrNull())
        }
    }

    @Nested
    @DisplayName("map")
    inner class Map {
        @Test
        fun `map transforms Success value`() {
            val result = Resource.Success(3).map { it * 2 }
            assertEquals(6, result.getOrNull())
        }

        @Test
        fun `map preserves Error message and throwable`() {
            val e = IllegalStateException("bad")
            val result: Resource<Int> = (Resource.Error("oops", e) as Resource<Int>).map { it * 2 }
            assertTrue(result is Resource.Error)
            assertEquals("oops", (result as Resource.Error).message)
            assertSame(e, result.throwable)
        }

        @Test
        fun `map passes through Loading unchanged`() {
            val input: Resource<Int> = Resource.Loading
            val result = input.map { it * 2 }
            assertSame(Resource.Loading, result)
        }

        @Test
        fun `map transform not invoked for non-Success states`() {
            var invoked = 0
            Resource.Loading.map<Any> { invoked++; it }
            (Resource.Error("x") as Resource<Int>).map { invoked++; it }
            assertEquals(0, invoked)
        }
    }

    @Nested
    @DisplayName("onSuccess/onError/onLoading side-effect callbacks")
    inner class SideEffects {
        @Test
        fun `onSuccess invoked only for Success`() {
            val seen = mutableListOf<Int>()
            Resource.Success(1).onSuccess { seen.add(it) }
            (Resource.Error("x") as Resource<Int>).onSuccess { seen.add(it) }
            (Resource.Loading as Resource<Int>).onSuccess { seen.add(it) }
            assertEquals(listOf(1), seen)
        }

        @Test
        fun `onError invoked only for Error`() {
            val seen = mutableListOf<String>()
            (Resource.Success(1) as Resource<Int>).onError { seen.add(it) }
            (Resource.Error("boom") as Resource<Int>).onError { seen.add(it) }
            (Resource.Loading as Resource<Int>).onError { seen.add(it) }
            assertEquals(listOf("boom"), seen)
        }

        @Test
        fun `onLoading invoked only for Loading`() {
            var count = 0
            (Resource.Success(1) as Resource<Int>).onLoading { count++ }
            (Resource.Error("x") as Resource<Int>).onLoading { count++ }
            (Resource.Loading as Resource<Int>).onLoading { count++ }
            assertEquals(1, count)
        }

        @Test
        fun `callbacks return this for chaining`() {
            val r: Resource<Int> = Resource.Success(1)
            assertSame(r, r.onSuccess {})
            assertSame(r, r.onError {})
            assertSame(r, r.onLoading {})
        }
    }
}
