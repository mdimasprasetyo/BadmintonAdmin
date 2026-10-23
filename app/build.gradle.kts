plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.mdp.badmintonadmin"

    // Menggunakan compileSdk Android 17 (API 37) sesuai persyaratan library terbaru
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mdp.badmintonadmin"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes from environment variables or Gradle properties (for example in
    // ~/.gradle/gradle.properties or CI secrets), never from files in this repository:
    //   BADMINTON_KEYSTORE_PATH, BADMINTON_KEYSTORE_PASSWORD, BADMINTON_KEY_ALIAS, BADMINTON_KEY_PASSWORD
    // When they are absent the release build falls back to the debug key so it still builds.
    fun signingValue(name: String): String? =
        (System.getenv(name) ?: project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }

    val keystorePath = signingValue("BADMINTON_KEYSTORE_PATH")
    val keystorePassword = signingValue("BADMINTON_KEYSTORE_PASSWORD")
    val keyAliasValue = signingValue("BADMINTON_KEY_ALIAS")
    val keyPasswordValue = signingValue("BADMINTON_KEY_PASSWORD")
    val hasReleaseSigning =
        keystorePath != null && keystorePassword != null && keyAliasValue != null && keyPasswordValue != null

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystorePath!!) // absolute, or relative to the repo root
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Debug/test builds install next to the real app instead of replacing it,
            // so trying a test APK never forces an uninstall that wipes the club roster.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("BadmintonAdmin: BADMINTON_KEYSTORE_* not set; release build is signed with the debug key.")
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- JETPACK COMPOSE & CORE ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // --- LOCAL STORAGE (ROOM DATABASE) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // HANYA gunakan ksp, buang annotationProcessor agar tidak konflik
    ksp(libs.androidx.room.compiler)

    // --- DEBUG SETUP (COMPOSE PREVIEW) ---
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- LOCAL UNIT TESTING ---
    testImplementation(libs.junit)

    // --- ANDROID INSTRUMENTATION TESTING ---
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

// Konfigurasi KSP untuk mengabaikan test pipelines agar tidak memicu bug variant AGP 9.x
ksp {
    arg("ksp.exclude.test", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}
