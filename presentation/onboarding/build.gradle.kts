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
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "app.hoop.presentation.onboarding"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.hoop.presentation.onboarding.resources"
    generateResClass = always
}
