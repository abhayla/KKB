package com.rasoiai.data.local

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @BeforeEach
    fun setup() {
        converters = Converters()
    }

    @Test
    @DisplayName("Should convert string list to JSON and back")
    fun `should convert string list to JSON and back`() {
        // Given
        val originalList = listOf("vegetarian", "jain", "north")

        // When
        val json = converters.fromStringList(originalList)
        val result = converters.toStringList(json)

        // Then
        assertEquals(originalList, result)
    }

    @Test
    @DisplayName("Should handle empty string list")
    fun `should handle empty string list`() {
        // Given
        val emptyList = emptyList<String>()

        // When
        val json = converters.fromStringList(emptyList)
        val result = converters.toStringList(json)

        // Then
        assertEquals(emptyList, result)
    }

    @Test
    @DisplayName("Should handle null string list")
    fun `should handle null string list`() {
        // When
        val json = converters.fromStringList(null)
        val result = converters.toStringList(json)

        // Then
        assertEquals(emptyList<String>(), result)
    }

    @Test
    @DisplayName("Should convert map to JSON and back")
    fun `should convert map to JSON and back`() {
        // Given
        val originalMap = mapOf("key1" to "value1", "key2" to "value2")

        // When
        val json = converters.fromMap(originalMap)
        val result = converters.toMap(json)

        // Then
        assertEquals(originalMap, result)
    }

    @Test
    @DisplayName("Should handle empty map")
    fun `should handle empty map`() {
        // Given
        val emptyMap = emptyMap<String, String>()

        // When
        val json = converters.fromMap(emptyMap)
        val result = converters.toMap(json)

        // Then
        assertEquals(emptyMap, result)
    }
}
