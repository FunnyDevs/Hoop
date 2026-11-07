plugins {
    id("hoop.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The domain only depends on Kotlin and coroutines: no framework, no other module.
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

android {
    namespace = "app.hoop.domain"
}
