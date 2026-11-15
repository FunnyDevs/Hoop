import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Settings are read from local.properties (not versioned) or Gradle properties. No address or key in the code.
fun setting(key: String): String? {
    val local = Properties().apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
    return local.getProperty(key) ?: providers.gradleProperty(key).orNull
}

fun parseSetting(key: String): String = setting(key).orEmpty()

// Parse server of a flavor: hoop.parse.<flavor>.<name>, else hoop.parse.<name> (a server shared by every flavor).
fun flavorParseSetting(flavor: String, name: String): String =
    setting("hoop.parse.$flavor.$name") ?: setting("hoop.parse.$name").orEmpty()

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
            implementation(project(":presentation:home"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kermit)
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
        manifestPlaceholders["appLabel"] = "Hoop"
        buildConfigField("String", "MAP_STYLE_URI", "\"${parseSetting("hoop.map.styleUri")}\"")
        buildConfigField("double", "MAP_FALLBACK_LATITUDE", parseSetting("hoop.map.fallbackLatitude").ifBlank { "0" })
        buildConfigField("double", "MAP_FALLBACK_LONGITUDE", parseSetting("hoop.map.fallbackLongitude").ifBlank { "0" })
    }
    // One build per server: local (the Parse Server on this Mac), staging and production. Local and staging get their own
    // package name, so they can be installed next to the real app.
    flavorDimensions += "server"
    productFlavors {
        create("local") {
            dimension = "server"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            manifestPlaceholders["appLabel"] = "Hoop Local"
        }
        create("staging") {
            dimension = "server"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            manifestPlaceholders["appLabel"] = "Hoop Staging"
        }
        create("production") {
            dimension = "server"
        }
        configureEach {
            val flavor = name
            buildConfigField("String", "PARSE_BASE_URL", "\"${flavorParseSetting(flavor, "baseUrl")}\"")
            buildConfigField("String", "PARSE_APPLICATION_ID", "\"${flavorParseSetting(flavor, "applicationId")}\"")
            buildConfigField("String", "PARSE_CLIENT_KEY", "\"${flavorParseSetting(flavor, "clientKey")}\"")
        }
    }
    // Kotlin Multiplatform names the flavor folders "android<Flavor>": point Android's source set to it explicitly.
    sourceSets.getByName("local") {
        manifest.srcFile("src/androidLocal/AndroidManifest.xml")
        res.srcDir("src/androidLocal/res")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Architecture rules are verified on every build.
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(":checkArchitecture") }
