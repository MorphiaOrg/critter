package dev.morphia.critter

import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.java.JavaCriteriaBuilder
import java.io.File
import java.util.Locale
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import dev.morphia.critter.java.CodecsBuilder as JavaCodecsBuilder

object Critter {
    private val LOG: Logger = LoggerFactory.getLogger(Critter::class.java)
    internal val DEFAULT_PACKAGE = "dev.morphia.critter.codecs"
    lateinit var javaContext: JavaContext

    fun scan(baseDir: File, sourceDirectories: Set<String>, criteriaPackage: String?, format: Boolean,
             sourceOutput: File, resourceOutput: File) {
        javaContext = JavaContext(criteriaPackage, format, sourceOutput, resourceOutput)
        sourceDirectories
            .map {
                val file = File(it)
                val mapped = if (file.isRooted) file else File(baseDir, it)
                mapped
            }
            .filter {
                it.exists()
            }
            .forEach { dir ->
                LOG.info("Scanning $dir for classes")
                dir.walk()
                    .filter { file -> file.extension in listOf("java") }
                    .forEach { file ->
                        javaContext.scan(file)
                    }
            }
    }

    fun generateCriteria() {
        JavaCriteriaBuilder(javaContext).build()
    }

    fun generateCodecs() {
        JavaCodecsBuilder(javaContext).build()
    }

    fun JavaClassSource.addMethods(methods: String): List<MethodSource<JavaClassSource>> {
        val parse = Roaster.parse("""class ${name} {
                $methods            
            }
            """.trimIndent()) as JavaClassSource
        val list = parse.methods
        return list.map {
            addMethod(it)
        }
    }
}

fun String.titleCase(): String {
    return first().uppercase(Locale.getDefault()) + substring(1)
}

fun String.methodCase(): String {
    return first().lowercase(Locale.getDefault()) + substring(1)
}

private val snakeCaseRegex = Regex("(?<=.)[A-Z]")
fun String.snakeCase(): String {
    return snakeCaseRegex.replace(this, "_$0").lowercase(Locale.getDefault())
}
