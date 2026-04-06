plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ost.application"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ost.application"
        minSdk = 30
        targetSdk = 36
        versionCode = 300
        versionName = "3.0.0-beta2"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    buildToolsVersion = "36.1.0"

    packaging  {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val horologist = "0.8.3-alpha"
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
    implementation(libs.coil.compose.v340)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation(libs.coil.compose)
    implementation("com.google.android.horologist:horologist-media-ui-model:$horologist")
    implementation("com.google.android.horologist:horologist-audio-ui-model:$horologist")
    implementation("com.google.android.horologist:horologist-audio-ui-material3:$horologist")
    implementation("com.google.android.horologist:horologist-media3-backend:$horologist")
    implementation("com.google.android.horologist:horologist-media3-logging:$horologist")
    implementation("com.google.android.horologist:horologist-media-ui-material3:$horologist")
    implementation("com.google.android.horologist:horologist-media3-outputswitcher:$horologist")
    implementation("com.google.android.horologist:horologist-media-data:$horologist")
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
    testImplementation(libs.horologist.roboscreenshots)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.animation.graphics.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}