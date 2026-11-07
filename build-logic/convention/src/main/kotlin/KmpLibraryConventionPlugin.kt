import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Common configuration of every Kotlin Multiplatform library module (Android + iOS).
 * Each module only declares its namespace and dependencies.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.library")

        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
            listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { ios ->
                ios.binaries.framework {
                    baseName = project.name
                    isStatic = true
                }
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
        }

        extensions.configure<LibraryExtension> {
            compileSdk = 35
            defaultConfig.minSdk = 24
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
