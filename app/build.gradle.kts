plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.25"
}
android {
    namespace = "cl.profe.mixaomitv"
    compileSdk = 35
    defaultConfig {
        applicationId = "cl.profe.mixaomitv"
        minSdk = 23
        targetSdk = 28
        versionCode = 3
        versionName = "3.0-cyberpunk"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
