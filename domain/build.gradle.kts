plugins {
    id("hoop.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The domain only depends on Kotlin and coroutines: no framework, no other module.
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "app.hoop.domain"
}
