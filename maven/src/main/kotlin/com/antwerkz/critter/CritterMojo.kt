package com.antwerkz.critter

import com.antwerkz.critter.java.JavaClass
import com.antwerkz.critter.kotlin.KotlinClass
import com.antwerkz.kibble.Kibble
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
import java.util.Arrays.asList


@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterMojo : AbstractMojo() {
    @Parameter
    private var sourceDirectory: Set<File> = setOf(File("src/main/java"), File("src/main/kotlin"))

    @Parameter(property = "critter.output.directory", defaultValue = "\${project.build.directory}/generated-sources/critter", readonly = true, required = true)
    private lateinit var outputDirectory: File

    @Parameter(property = "critter.criteria.package")
    private var criteriaPackage: String? = null

    @Parameter(property = "critter.force", defaultValue = "false")
    private var force: Boolean = false

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        project.addCompileSourceRoot(outputDirectory.path)

        val context = CritterContext(criteriaPackage, force)
        sourceDirectory.forEach {

            val walker = DirectoryWalker()
            walker.baseDir = it
            walker.includes = asList("**/*.java", "**/*.kt")

            walker.addDirectoryWalkListener(object : DirectoryWalkListener {
                override fun directoryWalkStarting(basedir: File) {}

                override fun directoryWalkStep(percentage: Int, file: File) {
                    if (file.name.endsWith(".java") && !file.name.endsWith("Criteria.java")) {
                        JavaClass(context, file).apply {
                            context.add(this)
                        }
                    } else if (file.name.endsWith(".kt") && !file.name.endsWith("Criteria.kt")) {
                        Kibble.parseFile(file).classes.forEach {
                            context.add(KotlinClass(context, it))
                        }
                    }
                }

                override fun directoryWalkFinished() {}

                override fun debug(message: String) {}
            })
            walker.scan()
        }
        context.classes.values.forEach { it.build(outputDirectory) }
    }
}