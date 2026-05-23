plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Room er jonno eta must
}

android {
    namespace = "com.notilogger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.notilogger"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- Notun Add Kora Holo ---
    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks") // Ager step e banano file
            storePassword = "kickMyAss" // Ekhane tomar password likho
            keyAlias = "my-key-alias"
            keyPassword = "kickMyAss" // Ekhane o same password likho
        }
    }

    buildTypes {
        release {
            // Release build ke signing config chinano holo
            signingConfig = signingConfigs.getByName("release")
            
            isMinifyEnabled = false 
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    // ---------------------------
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}



dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // Coroutines support
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Coroutines (Database background a chalanor jonno)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
