package dev.morphia.critter

import dev.morphia.critter.OutputType.JAVA
import dev.morphia.critter.OutputType.KOTLIN
import dev.morphia.critter.java.JavaBuilder
import dev.morphia.critter.java.JavaClass
import dev.morphia.critter.kotlin.KotlinBuilder
import dev.morphia.critter.kotlin.KotlinContext
import dev.morphia.critter.kotlin.KotlinParser
import org.codehaus.plexus.util.DirectoryWalkListener
import org.codehaus.plexus.util.DirectoryWalker
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.slf4j.LoggerFactory
import java.io.File

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
            .map { File(baseDir, it) }
            .filter { it.exists() }
            .forEach {
                val walker = DirectoryWalker()
                walker.baseDir = it
                walker.includes = listOf("**/*.java", "**/*.kt")

                LOG.info("Scanning $it for classes")
                walker.addDirectoryWalkListener(Walker(context, kotlinParser))
                walker.scan()
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
            OutputType.valueOf(name.toUpperCase())
        } catch (_: Exception) {
            throw IllegalArgumentException("Output type of '$name' is not supported.")
        }
    }
}

private class Walker(private val context: CritterContext,
                     private val kotlinParser: KotlinParser)
    : DirectoryWalkListener {
    override fun directoryWalkStarting(basedir: File) {}
    override fun directoryWalkStep(percentage: Int, file: File) {
        if (file.name.endsWith(".java") && !file.name.endsWith("Criteria.java")) {
            context.add(JavaClass(context, file))
        } else if (file.name.endsWith(".kt") && !file.name.endsWith("Criteria.kt")) {
            kotlinParser.parse(file)
        }
    }

    override fun directoryWalkFinished() {}
    override fun debug(message: String) {}
}
