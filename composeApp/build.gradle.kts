import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Parse server configuration: read from local.properties (not versioned) or Gradle properties.
// No address or key in the code.
fun parseSetting(key: String): String {
    val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
    return local.getProperty(key) ?: providers.gradleProperty(key).orNull.orEmpty()
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // iOS targets only build on macOS with Xcode installed.
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(project(":core:designsystem"))
            implementation(project(":presentation:onboarding"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "app.hoop"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.hoop"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "PARSE_BASE_URL", "\"${parseSetting("hoop.parse.baseUrl")}\"")
        buildConfigField("String", "PARSE_APPLICATION_ID", "\"${parseSetting("hoop.parse.applicationId")}\"")
        buildConfigField("String", "PARSE_CLIENT_KEY", "\"${parseSetting("hoop.parse.clientKey")}\"")
        buildConfigField("String", "MAP_STYLE_URI", "\"${parseSetting("hoop.map.styleUri")}\"")
        buildConfigField("double", "MAP_FALLBACK_LATITUDE", parseSetting("hoop.map.fallbackLatitude").ifBlank { "0" })
        buildConfigField("double", "MAP_FALLBACK_LONGITUDE", parseSetting("hoop.map.fallbackLongitude").ifBlank { "0" })
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Architecture rules are verified on every build.
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(":checkArchitecture") }

compose.resources {
    publicResClass = false
    packageOfResClass = "app.hoop.resources"
    generateResClass = always
}
