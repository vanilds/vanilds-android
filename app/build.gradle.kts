plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing: the upload key comes from environment variables (GitHub Actions
// secrets), never from the repo. Without them the release build is left unsigned.
val uploadKeystore = System.getenv("ANDROID_UPLOAD_KEYSTORE")?.let { file(it) }
val signRelease = uploadKeystore?.exists() == true

android {
    namespace = "io.github.vanilds.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.vanilds.app"
        minSdk = 26
        targetSdk = 36
        // Every CI run gets a higher versionCode (Play rejects a reused one).
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (signRelease) {
            create("upload") {
                storeFile = uploadKeystore
                storePassword = System.getenv("ANDROID_UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("ANDROID_UPLOAD_KEY_PASSWORD")
                    ?: System.getenv("ANDROID_UPLOAD_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (signRelease) signingConfig = signingConfigs.getByName("upload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.webkit:webkit:1.14.0")
}
