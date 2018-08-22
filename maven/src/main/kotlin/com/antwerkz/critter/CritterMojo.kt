package com.antwerkz.critter

import com.antwerkz.critter.java.JavaBuilder
import com.antwerkz.critter.java.JavaClass
import com.antwerkz.critter.kotlin.KotlinBuilder
import com.antwerkz.critter.kotlin.KotlinContext
import com.antwerkz.critter.kotlin.KotlinParser
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.DirectoryWalkListener
import org.codehaus.plexus.util.DirectoryWalker
import org.slf4j.LoggerFactory
import java.io.File


@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterMojo : AbstractMojo() {
    companion object {
        val LOG = LoggerFactory.getLogger(CritterMojo::class.java)
    }
    @Parameter
    private var sourceDirectory = setOf("src/main/java", "src/main/kotlin")

    @Parameter(property = "critter.output.directory", defaultValue = "\${project.build.directory}/generated-sources/critter", readonly = true, required = true)
    private lateinit var outputDirectory: File

    @Parameter(property = "critter.criteria.package")
    private var criteriaPackage: String? = null

    @Parameter(property = "critter.force", defaultValue = "false")
    private var force: Boolean = false

    @Parameter(property = "critter.output.type", name="outputType", required = true)
    lateinit var outputType: String

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        project.addCompileSourceRoot(outputDirectory.path)

        val context = CritterContext(criteriaPackage, force)
        val kotlinContext = KotlinContext(criteriaPackage, force)
        val kotlinParser = KotlinParser(kotlinContext)
        sourceDirectory
                .map { File(project.basedir, it) }
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
            "java" -> JavaBuilder(context).build(outputDirectory)
            "kotlin" -> KotlinBuilder(kotlinContext).build(outputDirectory)
        }
    }
}

class Walker(private val context: CritterContext, private val kotlinParser: KotlinParser) : DirectoryWalkListener {
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