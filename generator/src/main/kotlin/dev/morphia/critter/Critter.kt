package dev.morphia.critter

import dev.morphia.critter.OutputType.JAVA
import dev.morphia.critter.OutputType.KOTLIN
import dev.morphia.critter.java.JavaBuilder
import dev.morphia.critter.kotlin.KotlinBuilder
import dev.morphia.critter.kotlin.KotlinContext
import dev.morphia.critter.kotlin.KotlinParser
import java.io.File
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.slf4j.LoggerFactory

object Critter {
    val LOG = LoggerFactory.getLogger(Critter::class.java)

    fun scan(
        baseDir: File,
        sourceDirectory: Set<String>,
        criteriaPackage: String?,
        force: Boolean,
        outputType: OutputType,
        outputDirectory: File
    ) {
        val context = CritterContext(criteriaPackage, force)
        val kotlinContext = KotlinContext(criteriaPackage, force)
        val kotlinParser = KotlinParser(kotlinContext)
        sourceDirectory
            .map {
                var file = File(it)
                if (!file.isAbsolute && !file.exists()) {
                    file = File(baseDir, it)
                }
                file
            }
            .filter { it.exists() }
            .forEach {
                LOG.info("Scanning $it for classes")
                it.walk()
                    .filter { file ->
                        file.extension in listOf("java", "kt")
                    }
                    .forEach { file ->
                        if (file.name.endsWith(".java") && !file.name.endsWith("Criteria.java")) {
                            context.add(file)
                        } else if (file.name.endsWith(".kt") && !file.name.endsWith("Criteria.kt")) {
                            kotlinParser.parse(file)
                        }
                    }
            }

        when (outputType) {
            JAVA -> JavaBuilder(context).build(outputDirectory)
            KOTLIN -> KotlinBuilder(kotlinContext).build(outputDirectory)
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
            OutputType.valueOf(name.uppercase())
        } catch (_: Exception) {
            throw IllegalArgumentException("Output type of '$name' is not supported.")
        }
    }
}