plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ost.application"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ost.application"
        minSdk = 26
        targetSdk = 35
        versionCode = 242
        versionName = "2.4.2"

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

//        ndk {
//            //noinspection ChromeOsAbiSupport
//            abiFilters += "arm64-v8a"; "armeabi-v7a"
//        }

    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
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
    buildToolsVersion = "35.0.0"

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
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
    exclude ("com.google.android.material", "material")
}

dependencies {

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("sesl.androidx.core:core:1.15.0+1.0.11-sesl6+rev0")
    implementation("sesl.androidx.core:core-ktx:1.15.0+1.0.0-sesl6+rev0")
    implementation("sesl.androidx.fragment:fragment:1.8.4+1.0.0-sesl6+rev1")
    implementation("sesl.androidx.appcompat:appcompat:1.7.0+1.0.34-sesl6+rev7")
    implementation("sesl.androidx.picker:picker-basic:1.0.17+1.0.17-sesl6+rev2")
    implementation("sesl.androidx.picker:picker-color:1.0.6+1.0.6-sesl6+rev3")
    implementation("sesl.androidx.preference:preference:1.2.1+1.0.4-sesl6+rev3")
    implementation("sesl.androidx.recyclerview:recyclerview:1.4.0-rc01+1.0.21-sesl6+rev0")
    implementation("sesl.androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01+1.0.0-sesl6+rev0")
    implementation("sesl.androidx.apppickerview:apppickerview:1.0.1+1.0.1-sesl6+rev3")
    implementation("sesl.androidx.indexscroll:indexscroll:1.0.3+1.0.3-sesl6+rev3")
    implementation("sesl.androidx.viewpager2:viewpager2:1.1.0+1.0.0-sesl6+rev0")
    implementation("sesl.com.google.android.material:material:1.12.0+1.0.23-sesl6+rev2")
    implementation("sesl.androidx.slidingpanelayout:slidingpanelayout:1.2.0+1.0.2-sesl6+rev4")
    implementation("io.github.tribalfs:oneui-design:0.3.6+oneui6")
    implementation("io.github.oneuiproject:icons:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.airbnb.android:lottie:6.6.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.asynclayoutinflater:asynclayoutinflater:1.0.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    implementation("com.github.topjohnwu.libsu:nio:6.0.0")
    implementation("com.jaredrummler:android-device-names:2.1.1")
    implementation("com.google.android.play:integrity:1.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.oshi:oshi-core:6.6.5") {
        exclude("net.java.dev.jna", "jna")
    }
    implementation("net.java.dev.jna:jna:5.15.0@aar")
    implementation("androidx.datastore:datastore-preferences-android:1.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.compose.runtime:runtime:1.7.6")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.6")
    implementation("androidx.compose.runtime:runtime-rxjava2:1.7.6")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("com.android.volley:volley:1.2.1")
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}