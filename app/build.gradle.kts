plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ru.milvas.lkipia"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ru.milvas.lkipia"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    
    applicationVariants.all {
        outputs.all {
            val appName = "laboratorykipia" // желаемое имя файла
            val buildType = name // debug или release
            // val versionName = variant.versionName
            // val versionCode = variant.versionCode
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                // "${appName}_${versionName}_${versionCode}_${buildType}.apk"
                "${appName}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}