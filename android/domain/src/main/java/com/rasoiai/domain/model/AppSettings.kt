package com.rasoiai.domain.model

/**
 * App-level settings for user preferences.
 */
data class AppSettings(
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val mealReminderTime: String = "07:00",
    val volumeUnit: VolumeUnit = VolumeUnit.INDIAN,
    val weightUnit: WeightUnit = WeightUnit.METRIC,
    val smallMeasurementUnit: SmallMeasurementUnit = SmallMeasurementUnit.INDIAN
)

enum class DarkModePreference(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class VolumeUnit(val displayName: String) {
    METRIC("Metric (ml, L)"),
    US("US (cups, fl oz)"),
    INDIAN("Indian (katori, glass)")
}

enum class WeightUnit(val displayName: String) {
    METRIC("Metric (g, kg)"),
    US("US (oz, lbs)")
}

enum class SmallMeasurementUnit(val displayName: String) {
    METRIC("Metric (tsp, tbsp)"),
    INDIAN("Indian (chammach)")
}
