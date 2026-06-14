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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            // Tempat kustomisasi konfigurasi debug jika diperlukan nanti
        }
        release {
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
