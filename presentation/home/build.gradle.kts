plugins {
    id("hoop.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Presentation: depends on the domain and the design system, never on the data module.
            implementation(project(":domain"))
            implementation(project(":core:designsystem"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }
        // The live camera: CameraX on Android (AVFoundation on iOS needs no library).
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.video)
            implementation(libs.camerax.view)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "app.hoop.presentation.home"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.hoop.presentation.home.resources"
    generateResClass = always
}
