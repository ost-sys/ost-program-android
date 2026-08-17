plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ost.application"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ost.application"
        minSdk = 26
        targetSdk = 37
        versionCode = 300
        versionName = "3.0.0-beta6"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "../ost_key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
    buildToolsVersion = "37.0.0"

    packaging  {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation("io.legere:pdfiumandroid:2.0.1")

    val horologist = "0.8.3-alpha"
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation("com.google.android.horologist:horologist-media-ui-model:$horologist")
    implementation("com.google.android.horologist:horologist-audio-ui-model:$horologist")
    implementation("com.google.android.horologist:horologist-audio-ui-material3:$horologist")
    implementation("com.google.android.horologist:horologist-media3-backend:$horologist")
    implementation("com.google.android.horologist:horologist-media3-logging:$horologist")
    implementation("com.google.android.horologist:horologist-media-ui-material3:$horologist")
    implementation("com.google.android.horologist:horologist-media3-outputswitcher:$horologist")
    implementation("com.google.android.horologist:horologist-media-data:$horologist")
    implementation("com.google.android.horologist:horologist-images-coil:$horologist")
    implementation("com.google.android.horologist:horologist-tiles:$horologist")
    implementation("androidx.wear:wear-ongoing:1.1.0")
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.compose.material)
    implementation(libs.horologist.media)
    implementation(libs.horologist.media.ui)
    implementation(libs.horologist.audio)
    implementation(libs.horologist.audio.ui)
    implementation(libs.horologist.composables)
    implementation("androidx.palette:palette-ktx:1.0.0")
    testImplementation(libs.horologist.roboscreenshots)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.activity)
    implementation(project(":core"))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}