plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "calculator.hide.vault"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "calculator.hide.vault"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Security Crypto (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)
    
    // ViewPager2
    implementation(libs.androidx.viewpager2)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(files("libs/unityLibrary-debug.aar"))

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.0"))
    api("com.chafennet.uaas:lib-for-calculator.hide.vault-google:1.0.0")
}