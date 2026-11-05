plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "local.volumenmaestro"
    compileSdk = 34

    defaultConfig {
        applicationId = "local.volumenmaestro"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "2.4"
    }

    // La misma clave que la versión anterior: así se instala como actualización y conserva
    // la autorización de depuración y el servicio de accesibilidad habilitado.
    signingConfigs {
        create("tv") {
            storeFile = rootProject.file("keystore.jks")
            storePassword = "android"
            keyAlias = "vm"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("tv")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("tv")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303") // org.json real para los tests (el de Android es un stub)
}
