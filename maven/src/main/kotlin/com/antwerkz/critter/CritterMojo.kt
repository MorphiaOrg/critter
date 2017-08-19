package com.antwerkz.critter

import com.antwerkz.critter.java.JavaBuilder
import com.antwerkz.critter.java.JavaClass
import com.antwerkz.critter.kotlin.KotlinBuilder
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
import java.io.File


@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterMojo : AbstractMojo() {
    @Parameter
    private var sourceDirectory = setOf("src/main/java", "src/main/kotlin")

    @Parameter(property = "critter.output.directory", defaultValue = "\${project.build.directory}/generated-sources/critter", readonly = true, required = true)
    private lateinit var outputDirectory: File

    @Parameter(property = "critter.criteria.package")
    private var criteriaPackage: String? = null

    @Parameter(property = "critter.force", defaultValue = "false")
    private var force: Boolean = false

    @Parameter(property = "critter.output.type", defaultValue = "kotlin")
    private var outputType = "kotlin"

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        project.addCompileSourceRoot(outputDirectory.path)

        val context = CritterContext(criteriaPackage, force)
        val kotlinParser = KotlinParser()
        sourceDirectory
                .map { File(project.basedir, it) }
                .filter { it.exists() }
                .forEach {
                    val walker = DirectoryWalker()
                    walker.baseDir = it
                    walker.includes = listOf("**/*.java", "**/*.kt")

                    walker.addDirectoryWalkListener(object : DirectoryWalkListener {
                        override fun directoryWalkStarting(basedir: File) {}

                        override fun directoryWalkStep(percentage: Int, file: File) {
                            if (file.name.endsWith(".java") && !file.name.endsWith("Criteria.java")) {
                                context.add(JavaClass(context, file))
                            } else if (file.name.endsWith(".kt") && !file.name.endsWith("Criteria.kt")) {
                                kotlinParser.parse(file).forEach {
                                    context.add(it)
                                }
                            }
                        }

                        override fun directoryWalkFinished() {}

                        override fun debug(message: String) {}
                    })
                    walker.scan()
                }

        when (outputType) {
            "java" -> {
                val builder = JavaBuilder(context)
                context.classes.values.forEach { builder.build(outputDirectory, it) }
            }
            "kotlin" -> {
                val builder = KotlinBuilder(context)
                context.classes.values.forEach { builder.build(outputDirectory, it) }
            }
        }
    }
}