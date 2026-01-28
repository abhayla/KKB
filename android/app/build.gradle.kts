import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Load local.properties for sensitive configuration
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

// Get Web Client ID from local.properties or environment variable (for CI)
val webClientId: String = localProperties.getProperty("WEB_CLIENT_ID")
    ?: System.getenv("WEB_CLIENT_ID")
    ?: throw GradleException(
        "WEB_CLIENT_ID not found. Please add it to local.properties or set as environment variable.\n" +
        "See local.properties.example for reference."
    )

android {
    namespace = "com.rasoiai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rasoiai.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.rasoiai.app.HiltTestRunner"
        // Disabled clearPackageData to avoid test isolation issues with Compose
        // testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Web Client ID for Google Sign-In (loaded from local.properties or environment variable)
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
    }

    signingConfigs {
        create("release") {
            // TODO: Configure release signing
            // storeFile = file("../keystore/release.keystore")
            // storePassword = System.getenv("KEYSTORE_PASSWORD")
            // keyAlias = System.getenv("KEY_ALIAS")
            // keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            // Removed applicationIdSuffix to match Firebase config
            // Add com.rasoiai.app.debug to Firebase Console for production
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    // Compose compiler stability configuration
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
                "${projectDir.absolutePath}/compose-stability.conf"
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkDependencies = true
    }

    testOptions {
        // Disabled Test Orchestrator temporarily to fix Compose test issues
        // execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }
}

dependencies {
    // Project modules
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.activity.compose)

    // Splash Screen (Android 12+)
    implementation(libs.splashscreen)

    // Baseline Profiles (startup performance optimization)
    implementation(libs.profileinstaller)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Google Sign-In / Credentials
    implementation(libs.play.services.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // WorkManager
    implementation(libs.work.runtime)

    // Image Loading
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Logging
    implementation(libs.timber)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Immutable Collections (Compose stability)
    implementation(libs.kotlinx.collections.immutable)

    // Memory Leak Detection (debug only)
    debugImplementation(libs.leakcanary)

    // Testing
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.work.testing)

    // Tracing - explicit version to fix test NoSuchMethodError: forceEnableAppTracing
    implementation("androidx.tracing:tracing:1.2.0")

    // Android instrumented tests
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.compiler)

    // Test Orchestrator (prevents TransactionTooLargeException)
    androidTestUtil(libs.androidx.test.orchestrator)
}
