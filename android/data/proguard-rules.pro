# ProGuard rules for data module

# Keep Room entities — Room uses reflection for schema generation
-keep class com.rasoiai.data.local.entity.** { *; }

# Keep Retrofit DTOs — Gson uses reflection for serialization
-keep class com.rasoiai.data.remote.dto.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.rasoiai.data.remote.api.* {
    @retrofit2.http.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
