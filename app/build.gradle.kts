plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.twinmind.voicenotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.twinmind.voicenotes"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        fun prop(name: String) = (project.findProperty(name) as String? ?: "")
        buildConfigField("String", "AZURE_OPENAI_API_KEY", "\"${prop("AZURE_OPENAI_API_KEY")}\"")
        buildConfigField("String", "AZURE_OPENAI_ENDPOINT", "\"${prop("AZURE_OPENAI_ENDPOINT")}\"")
        buildConfigField("String", "AZURE_DEPLOYMENT_TRANSCRIBE", "\"${prop("AZURE_DEPLOYMENT_TRANSCRIBE")}\"")
        buildConfigField("String", "AZURE_API_VERSION", "\"${prop("AZURE_API_VERSION")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
}
