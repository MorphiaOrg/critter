package dev.morphia.critter

import dev.morphia.critter.OutputType.JAVA
import dev.morphia.critter.OutputType.KOTLIN
import dev.morphia.critter.java.JavaClass
import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.kotlin.KotlinContext
import dev.morphia.critter.kotlin.KotlinCriteriaBuilder
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale
import dev.morphia.critter.java.CodecsBuilder as JavaCodecsBuilder
import dev.morphia.critter.java.CriteriaBuilder as JavaCriteriaBuilder
import dev.morphia.critter.kotlin.CodecsBuilder as KotlinCodecsBuilder

object Critter {
    private val LOG: Logger = LoggerFactory.getLogger(Critter::class.java)
    private lateinit var kotlinContext: KotlinContext
    private lateinit var javaContext: JavaContext
    private lateinit var outputType: OutputType

    fun scan(
        baseDir: File,
        sourceDirectories: Set<String>,
        criteriaPackage: String?,
        force: Boolean,
        format: Boolean,
        outputType: OutputType,
        outputDirectory: File
    ) {
        javaContext = JavaContext(criteriaPackage, force, format, outputDirectory)
        kotlinContext = KotlinContext(criteriaPackage, force, format, outputDirectory)
        this.outputType = outputType
        sourceDirectories
            .map {
                val file = File(it)
                if (file.isRooted) file else File(baseDir, it)
            }
            .filter {
                it.exists()
            }
            .forEach { dir ->
                LOG.info("Scanning $dir for classes")
                dir.walk()
                    .filter { file -> file.extension in listOf("java", "kt") }
                    .forEach { file ->
                        if (file.name.endsWith(".java") && !file.name.endsWith("Criteria.java")) {
                            javaContext.add(file)
                        } else if (file.name.endsWith(".kt") && !file.name.endsWith("Criteria.kt")) {
                            kotlinContext.add(file)
                        }
                    }
            }
    }

    fun generateCriteria() {
        when (outputType) {
            JAVA -> JavaCriteriaBuilder(javaContext).build()
            KOTLIN -> KotlinCriteriaBuilder(kotlinContext).build()
        }
    }

    fun generateCodecs() {
        when (outputType) {
            JAVA -> JavaCodecsBuilder(javaContext).build()
            KOTLIN -> KotlinCodecsBuilder(kotlinContext).build()
        }
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
