import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Automatic detection of architectural anti-patterns: dependency rules between modules and bans
 * (hardcoded text, colors outside the design system, addresses in code). `./gradlew checkArchitecture`.
 */
class ArchitectureCheckPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register("checkArchitecture", CheckArchitectureTask::class.java) {
            group = "verification"
            description = "Verifies the architecture rules of the project."
            rootDir.set(target.layout.projectDirectory)
            outputs.upToDateWhen { false }
        }
    }
}

abstract class CheckArchitectureTask : DefaultTask() {
    @get:Internal
    abstract val rootDir: DirectoryProperty

    private class Rule(val name: String, val dir: String, val pattern: Regex, val message: String, val importsOnly: Boolean = false)

    private val designComponents = "core/designsystem/src/commonMain/kotlin/app/hoop/designsystem/component"
    private val designTheme = "core/designsystem/src/commonMain/kotlin/app/hoop/designsystem/theme"
    private val hardcodedText = Regex("""\bText\(\s*(text\s*=\s*)?"""")
    private val hexColor = Regex("""Color\(0x""")
    private val url = Regex("""https?://""")

    private val rules = listOf(
        Rule("domain-purity", "domain/src/commonMain",
            Regex("""^import\s+(androidx|android|io\.ktor|org\.koin|io\.reactivex|app\.hoop\.(data|presentation|designsystem))\b"""),
            "The domain cannot import frameworks or other layers", importsOnly = true),
        Rule("presentation-no-data", "presentation", Regex("""^import\s+(app\.hoop\.data|io\.ktor)\b"""),
            "Presentation cannot import the data module or Ktor", importsOnly = true),
        Rule("designsystem-no-business", "core/designsystem", Regex("""^import\s+app\.hoop\.(domain|data|presentation)\b"""),
            "The design system cannot import domain, data or presentation", importsOnly = true),
        Rule("data-no-ui", "data/src", Regex("""^import\s+(androidx\.compose|app\.hoop\.(presentation|designsystem))\b"""),
            "The data layer cannot import UI or the design system", importsOnly = true),
        Rule("no-hardcoded-text", "presentation", hardcodedText, "Hardcoded text: use stringResource"),
        Rule("no-hardcoded-text-components", designComponents, hardcodedText, "Hardcoded text in component: text comes from the caller"),
        Rule("no-hex-colors", "presentation", hexColor, "Hex color outside the design system: use HoopTheme.colors"),
        Rule("no-hex-colors-components", designComponents, hexColor, "Hex color in component: use tokens"),
        Rule("no-hex-colors-theme", designTheme, hexColor, "Hex color in theme: it belongs in HoopPalette"),
        Rule("no-urls-data", "data/src/commonMain", url, "Hardcoded address: configuration comes from ParseConfig"),
        Rule("no-urls-app", "composeApp/src", url, "Hardcoded address: configuration comes from outside"),
    )

    private val forbiddenBuildDeps = mapOf(
        "domain/build.gradle.kts" to Regex("""project\(":(data|presentation|core)"""),
        "core/designsystem/build.gradle.kts" to Regex("""project\(":(domain|data|presentation)"""),
        "presentation/onboarding/build.gradle.kts" to Regex("""project\(":data"\)"""),
        "data/build.gradle.kts" to Regex("""project\(":(presentation|core)"""),
    )

    @TaskAction
    fun check() {
        val root = rootDir.get().asFile
        val violations = mutableListOf<String>()
        rules.forEach { rule ->
            File(root, rule.dir).walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    val code = if (rule.importsOnly) line.trim() else line.substringBefore("//")
                    if (rule.pattern.containsMatchIn(code)) {
                        violations += "${rule.name}: ${file.relativeTo(root)}:${index + 1}  ${rule.message}"
                    }
                }
            }
        }
        forbiddenBuildDeps.forEach { (path, regex) ->
            val file = File(root, path)
            if (file.exists() && regex.containsMatchIn(file.readText())) {
                violations += "module-dependency: $path  dependency forbidden by the layering rule"
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Architecture violations:\n" + violations.joinToString("\n") { " - $it" })
        }
        println("Architecture OK: ${rules.size + forbiddenBuildDeps.size} rules satisfied")
    }
}
