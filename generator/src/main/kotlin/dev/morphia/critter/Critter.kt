package dev.morphia.critter

import dev.morphia.critter.OutputType.JAVA
import dev.morphia.critter.OutputType.KOTLIN
import dev.morphia.critter.java.CriteriaBuilder as JavaCriteriaBuilder
import dev.morphia.critter.java.JavaClass
import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.kotlin.KotlinCriteriaBuilder
import dev.morphia.critter.kotlin.KotlinContext
import dev.morphia.critter.kotlin.KotlinParser
import org.codehaus.plexus.util.DirectoryWalkListener
import org.codehaus.plexus.util.DirectoryWalker
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale
import dev.morphia.critter.java.CodecsBuilder as JavaCodecsBuilder

object Critter {
    private val LOG: Logger = LoggerFactory.getLogger(Critter::class.java)
    private lateinit var kotlinContext: KotlinContext
    private lateinit var javaContext: JavaContext
    private lateinit var kotlinParser: KotlinParser
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
        kotlinParser = KotlinParser(kotlinContext)
        sourceDirectories
            .map {
                val file = File(it)
                if(file.isRooted) file else File(baseDir, it)
            }
            .filter { it.exists() }
            .forEach {
                val walker = DirectoryWalker()
                walker.baseDir = it
                walker.includes = listOf("**/*.java", "**/*.kt")

                LOG.info("Scanning $it for classes")
                walker.addDirectoryWalkListener(Walker(javaContext, kotlinParser))
                walker.scan()
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
            KOTLIN -> println("not implemented yet") //KotlinCriteriaBuilder(kotlinContext).build()
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

private class Walker(private val context: JavaContext,
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
