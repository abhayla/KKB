package com.rasoiai.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Round-trip tests for the five enums declared in User.kt.
 *
 * Two of them (SpecialDietaryNeed, DietaryRestriction) return NULLABLE
 * from fromValue — contract that unknown values are filtered out, not
 * coerced to a default. The other three have defaults.
 */
class UserEnumsTest {

    @Nested
    @DisplayName("MemberType")
    inner class MemberTypeTests {
        @Test
        fun `round-trips every entry`() {
            MemberType.entries.forEach {
                assertEquals(it, MemberType.fromValue(it.value))
            }
        }

        @Test
        fun `unknown falls back to ADULT`() {
            assertEquals(MemberType.ADULT, MemberType.fromValue("extraterrestrial"))
        }

        @Test
        fun `all entries have non-empty displayName`() {
            MemberType.entries.forEach { assertEquals(it.displayName.isNotEmpty(), true) }
        }
    }

    @Nested
    @DisplayName("SpecialDietaryNeed")
    inner class SpecialDietaryNeedTests {
        @Test
        fun `round-trips every entry`() {
            SpecialDietaryNeed.entries.forEach {
                assertEquals(it, SpecialDietaryNeed.fromValue(it.value))
            }
        }

        @Test
        fun `unknown returns null (contract - no default)`() {
            // Nullable return — unknown needs should be filtered out by the
            // caller, not coerced to a default, because each need has distinct
            // downstream meaning (diabetes != low-sodium).
            assertNull(SpecialDietaryNeed.fromValue("quantum"))
        }
    }

    @Nested
    @DisplayName("PrimaryDiet")
    inner class PrimaryDietTests {
        @Test
        fun `round-trips every entry`() {
            PrimaryDiet.entries.forEach {
                assertEquals(it, PrimaryDiet.fromValue(it.value))
            }
        }

        @Test
        fun `unknown falls back to VEGETARIAN (safe default for Indian context)`() {
            // VEGETARIAN is the safest default — it guarantees the meal plan
            // won't include meat/fish that a user never intended.
            assertEquals(PrimaryDiet.VEGETARIAN, PrimaryDiet.fromValue("unknown"))
        }

        @Test
        fun `description is populated for every entry`() {
            // description is surfaced in the onboarding UI — blank would leave
            // users without guidance. Guard against accidental empty strings.
            PrimaryDiet.entries.forEach {
                assertEquals(it.description.isNotBlank(), true, "${it.name} has blank description")
            }
        }
    }

    @Nested
    @DisplayName("DietaryRestriction")
    inner class DietaryRestrictionTests {
        @Test
        fun `round-trips every entry`() {
            DietaryRestriction.entries.forEach {
                assertEquals(it, DietaryRestriction.fromValue(it.value))
            }
        }

        @Test
        fun `unknown returns null (contract - no default)`() {
            // Nullable — unknown restrictions are filtered rather than coerced.
            assertNull(DietaryRestriction.fromValue("mysterious-restriction"))
        }
    }

    @Nested
    @DisplayName("SpiceLevel")
    inner class SpiceLevelTests {
        @Test
        fun `round-trips every entry`() {
            SpiceLevel.entries.forEach {
                assertEquals(it, SpiceLevel.fromValue(it.value))
            }
        }

        @Test
        fun `unknown falls back to MEDIUM (safe default)`() {
            // MEDIUM is the middle ground — guards against accidentally
            // serving wildly-spicy food to someone whose preference was lost.
            assertEquals(SpiceLevel.MEDIUM, SpiceLevel.fromValue("nuclear"))
        }
    }
}
