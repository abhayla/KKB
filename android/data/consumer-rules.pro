# Consumer ProGuard rules for data module
# These rules will be applied to consumers of this library

# Keep Room entities
-keep class com.rasoiai.data.local.entity.** { *; }

# Keep DTOs for Gson
-keep class com.rasoiai.data.remote.dto.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.rasoiai.data.remote.api.* {
    @retrofit2.http.* <methods>;
}
