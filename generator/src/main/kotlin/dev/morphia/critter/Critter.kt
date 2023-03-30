package dev.morphia.critter

import dev.morphia.critter.java.JavaContext
import java.io.File
import java.util.Locale
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import dev.morphia.critter.java.CodecsBuilder as JavaCodecsBuilder
import dev.morphia.critter.java.CriteriaBuilder as JavaCriteriaBuilder

object Critter {
    private val LOG: Logger = LoggerFactory.getLogger(Critter::class.java)
    private lateinit var javaContext: JavaContext
    private lateinit var outputType: OutputType

    fun scan(baseDir: File, sourceDirectories: Set<String>, criteriaPackage: String?, force: Boolean, format: Boolean,
             outputType: OutputType, sourceOutput: File, resourceOutput: File) {
        javaContext = JavaContext(criteriaPackage, force, format, sourceOutput, resourceOutput)
        this.outputType = outputType
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

    fun outputType(name: String): OutputType {
        return try {
            OutputType.valueOf(name.uppercase(Locale.getDefault()))
        } catch (_: Exception) {
            throw IllegalArgumentException("Output type of '$name' is not supported.")
        }
    }
}

fun String.nameCase(): String {
    return first().uppercase(Locale.getDefault()) + substring(1)
}

fun String.titleCase(): String {
    return first().uppercase(Locale.getDefault()) + substring(1)
}

fun String.methodCase(): String {
    return first().lowercase(Locale.getDefault()) + substring(1)
}
