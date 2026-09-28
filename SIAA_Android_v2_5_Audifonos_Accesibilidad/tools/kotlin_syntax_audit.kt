import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CompilerConfiguration
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

fun main(args: Array<String>) {
    val root = Disposer.newDisposable()
    try {
        val env = KotlinCoreEnvironment.createForProduction(root, CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val factory = KtPsiFactory(env.project, false)
        var count=0; var bad=0
        File(args[0]).walkTopDown().filter { it.isFile && it.extension in listOf("kt","kts") && !it.path.contains("/build/") }.forEach { file ->
            count++
            val parsed = factory.createFile(file.name, file.readText())
            val errors = PsiTreeUtil.collectElementsOfType(parsed, PsiErrorElement::class.java)
            errors.forEach { error ->
                val line=file.readText().take(error.textOffset).count { it=='\n' }+1
                println("${file.path}:$line: ${error.errorDescription}")
                bad++
            }
        }
        println("Kotlin PSI syntax: $count files, $bad errors. Syntax only; not Android compilation.")
        check(bad==0)
    } finally { Disposer.dispose(root) }
}
