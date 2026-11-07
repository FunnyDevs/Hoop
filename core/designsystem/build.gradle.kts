plugins {
    id("hoop.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.maplibre.compose)
        }
    }
}

android {
    namespace = "app.hoop.designsystem"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "app.hoop.designsystem.resources"
    generateResClass = always
}
