plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    // Firebase — descomentar cuando agregues google-services.json
    // alias(libs.plugins.google.services)
}

android {
    namespace = "com.upn.app_vecinoalerta"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.upn.app_vecinoalerta"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room: exporta el esquema para migraciones
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", "true")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
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
        viewBinding = true   // ViewBinding para Activities y Fragments
    }
}

dependencies {
    // ── AndroidX Core ──────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)

    // ── Lifecycle (ViewModel + LiveData/Flow) ──────────────
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ── Navigation Component ───────────────────────────────
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ── Room (SQLite local — Single Source of Truth) ───────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)         // Soporte Coroutines + Flow
    kapt(libs.androidx.room.compiler)

    // ── Hilt (Inyección de dependencias) ──────────────────
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ── Coroutines ────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── WorkManager (sincronización en background) ────────
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)         // Hilt + WorkManager
    kapt(libs.androidx.hilt.compiler)

    // ── Seguridad: BCrypt para hash de contraseñas ────────
    // RNF-03: nunca almacenamos texto plano
    implementation(libs.jbcrypt)

    // ── Glide (carga de imágenes para evidencia incidencias)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // ── Firebase (fase 2 — sincronización cloud) ──────────
    // Descomentar cuando tengas google-services.json
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.firestore.ktx)
    // implementation(libs.firebase.auth.ktx)

    // ── Tests ─────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}