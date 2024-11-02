plugins {
    alias(libs.plugins.android.application)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
    id("org.jetbrains.kotlin.android") version "2.1.0-Beta2"
}

android {
    namespace = "com.ost.application"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ost.application"
        minSdk = 28
        targetSdk = 34
        versionCode = 200
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
    buildToolsVersion = "34.0.0"
}

buildscript {
    dependencies {
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:<version>-SNAPSHOT")
    }
}

configurations.implementation {
    exclude ("androidx.core",  "core")
    exclude ("androidx.core",  "core-ktx")
    exclude ("androidx.customview",  "customview")
    exclude ("androidx.coordinatorlayout",  "coordinatorlayout")
    exclude ("androidx.drawerlayout",  "drawerlayout")
    exclude ("androidx.viewpager2",  "viewpager2")
    exclude ("androidx.viewpager",  "viewpager")
    exclude ("androidx.appcompat", "appcompat")
    exclude ("androidx.fragment", "fragment")
    exclude ("androidx.preference",  "preference")
    exclude ("androidx.recyclerview", "recyclerview")
    exclude ("androidx.slidingpanelayout",  "slidingpanelayout")
    exclude ("androidx.swiperefreshlayout",  "swiperefreshlayout")
    exclude ("com.google.android.material.component", "material")
}

dependencies {

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("sesl.androidx.core:core:1.13.1+1.0.0-sesl6+rev0")
    implementation("sesl.androidx.core:core-ktx:1.13.1+1.0.0-sesl6+rev0")
    implementation("sesl.androidx.fragment:fragment:1.8.0+1.0.0-sesl6+rev0")
    implementation("sesl.androidx.appcompat:appcompat:1.7.0+1.0.28-sesl6+rev0")
    implementation("sesl.androidx.picker:picker-basic:1.0.16+1.0.16-sesl6+rev0")
    implementation("sesl.androidx.picker:picker-color:1.0.6+1.0.6-sesl6+rev3")
    implementation("sesl.androidx.preference:preference:1.2.1+1.0.4-sesl6+rev3")
    implementation("sesl.androidx.recyclerview:recyclerview:1.4.0-beta01+1.0.21-sesl6+rev2")
    implementation("sesl.androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01+1.0.0-sesl6+rev0")
    implementation("sesl.androidx.apppickerview:apppickerview:1.0.1+1.0.1-sesl6+rev2")
    implementation("sesl.androidx.indexscroll:indexscroll:1.0.3+1.0.3-sesl6+rev2")
    implementation("sesl.com.google.android.material:material:1.12.0+1.0.18-sesl6+rev0")
    implementation("sesl.androidx.viewpager2:viewpager2:1.1.0+1.0.0-sesl6+rev0")

//    implementation(project(":lib"))

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("io.github.oneuiproject:icons:1.1.0")
    implementation("androidx.webkit:webkit:1.12.1")

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:31.0.1-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.asynclayoutinflater:asynclayoutinflater:1.0.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    implementation("com.github.topjohnwu.libsu:nio:6.0.0")

    implementation("eu.chainfire:libsuperuser:1.1.1")

    implementation("io.github.tribalfs:oneui-design:0.1.3+oneui6")

}