plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.antigravityremote.wear"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.antigravityremote"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
      jniLibs {
        useLegacyPackaging = false
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  // Core Wear OS Dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Wear OS Compose (Tailored for Circular Smartwatches like Galaxy Watch Ultra)
  implementation("androidx.wear.compose:compose-material:1.3.1")
  implementation("androidx.wear.compose:compose-foundation:1.3.1")
  implementation("androidx.wear.compose:compose-navigation:1.3.1")
  implementation("androidx.wear:wear:1.3.0")

  // Play Services Wearable (Phone-to-Watch Bluetooth & Wi-Fi Sync)
  implementation("com.google.android.gms:play-services-wearable:18.2.0")

  // Local coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
