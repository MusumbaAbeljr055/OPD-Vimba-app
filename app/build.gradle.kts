plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.azimbalife"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.azimbalife"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    viewBinding {
        enable = true

    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.swiperefreshlayout)
    implementation(libs.gridlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.13.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.github.dhaval2404:imagepicker:2.1")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.google.android.material:material:1.9.0")
//    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")




}