import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val envProperties = Properties().apply {
    val file = rootProject.file(".env")
    if (file.exists()) file.inputStream().use { load(it) }
}

val googleWebClientId = (
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
        ?: envProperties.getProperty("GOOGLE_CLIENT_ID_ANDROID")
        ?: envProperties.getProperty("GOOGLE_CLIENT_ID")
    )?.trim().orEmpty()

val firebaseApiKey = (
    localProperties.getProperty("FIREBASE_API_KEY")
        ?: envProperties.getProperty("FIREBASE_API_KEY")
    )?.trim().orEmpty()

val apiBaseUrl = (
    localProperties.getProperty("API_BASE_URL")
        ?: envProperties.getProperty("API_BASE_URL")
        ?: "http://10.0.2.2:3000/"
    ).trim()

// Optional: Firebase API key for development builds
if (googleWebClientId.isBlank()) {
    logger.warn("GOOGLE_WEB_CLIENT_ID not found - Google Sign-In will not work")
}

if (firebaseApiKey.isBlank()) {
    logger.warn("FIREBASE_API_KEY not found - using placeholder for development")
}

android {
    namespace = "com.calico.tutor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.calico.tutor"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"$googleWebClientId\""
        )

        buildConfigField(
            "String",
            "FIREBASE_API_KEY",
            "\"$firebaseApiKey\""
        )

        buildConfigField(
            "String",
            "BASE_URL",
            "\"$apiBaseUrl\""
        )

        resValue("string", "google_web_client_id", googleWebClientId)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.java.jwt)

    // Image Loading
    implementation(libs.coil.compose)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Play Services for Google Sign-In
    implementation(libs.google.play.services.auth)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-analytics")
}
