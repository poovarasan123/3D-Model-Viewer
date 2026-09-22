import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

android {
    namespace = localProperties.getProperty("applicationId")
    compileSdk {
        version = release(localProperties.getProperty("compileSDK").toInt())
    }

    defaultConfig {
        applicationId = localProperties.getProperty("applicationId")
        minSdk = localProperties.getProperty("minSDK").toInt()
        targetSdk = localProperties.getProperty("targetSDK").toInt()
        versionCode = localProperties.getProperty("versionCode").toInt()
        versionName = localProperties.getProperty("versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("dev") {
            storeFile = localProperties.getProperty("signing.dev.storeFile")?.let { file(it) }
            storePassword = localProperties.getProperty("signing.dev.storePassword")
            keyAlias = localProperties.getProperty("signing.dev.keyAlias")
            keyPassword = localProperties.getProperty("signing.dev.keyPassword")
        }
        create("prod") {
            storeFile = localProperties.getProperty("signing.prod.storeFile")?.let { file(it) }
            storePassword = localProperties.getProperty("signing.prod.storePassword")
            keyAlias = localProperties.getProperty("signing.prod.keyAlias")
            keyPassword = localProperties.getProperty("signing.prod.keyPassword")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("prod")
            optimization {
                enable = false
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)



    implementation(libs.androidx.compose.material.icon.extended)

    implementation(libs.sceneview)

}