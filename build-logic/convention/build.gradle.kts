plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("hoopArchitecture") {
            id = "hoop.architecture"
            implementationClass = "ArchitectureCheckPlugin"
        }
        register("hoopKmpLibrary") {
            id = "hoop.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
    }
}
