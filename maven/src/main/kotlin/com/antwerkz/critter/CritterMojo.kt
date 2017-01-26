package com.antwerkz.critter

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.DirectoryWalkListener
import org.codehaus.plexus.util.DirectoryWalker
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import java.io.File
import java.io.FileNotFoundException
import java.util.Arrays.asList


@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterMojo : AbstractMojo() {
    @Parameter(defaultValue = "src/main/java", readonly = true, required = true)
    private lateinit var sourceDirectory: File

    @Parameter(property = "critter.output.directory", defaultValue = "\${project.build.directory}/generated-sources/critter", readonly = true, required = true)
    private lateinit var outputDirectory: File

    @Parameter(property = "critter.criteria.package")
    private val criteriaPackage: String? = null

    @Parameter(property = "critter.force", defaultValue = "false")
    private var force: Boolean = false

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        project.addCompileSourceRoot(outputDirectory.path)
        processJava()
//        processKotlin()
    }

    private fun processJava() {
        val context = CritterContext(criteriaPackage, force)
        val walker = DirectoryWalker()
        walker.baseDir = sourceDirectory
        walker.includes = asList("**/*.java")

        walker.addDirectoryWalkListener(object : DirectoryWalkListener {
            override fun directoryWalkStarting(basedir: File) {}

            override fun directoryWalkStep(percentage: Int, file: File) {
                if (!file.name.endsWith("Criteria.java")) {
                    val type = Roaster.parse(file)
                    context.add(type.getPackage(), CritterClass(context, file, type))
                }
            }

            override fun directoryWalkFinished() {}

            override fun debug(message: String) {}
        })
        walker.scan()
        context.classes.values
                .forEach { critterClass -> critterClass.build(outputDirectory) }
    }

    private fun processKotlin() {
        val context = CritterContext(criteriaPackage, force)
        val walker = DirectoryWalker()
        walker.baseDir = sourceDirectory
        walker.includes = asList("**/*.kt")

        try {
            walker.addDirectoryWalkListener(object : DirectoryWalkListener {
                override fun directoryWalkStarting(basedir: File) {}

                override fun directoryWalkStep(percentage: Int, file: File) {
                    if (!file.name.endsWith("Criteria.kt")) {
                        val type: JavaType<*>
                        try {
                            type = Roaster.parse(file)
                            context.add(type.getPackage(), CritterClass(context, file, type))
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace(System.out)
                            throw RuntimeException(e.message, e)
                        }
                    }
                }

                override fun directoryWalkFinished() {}

                override fun debug(message: String) {}
            })
            walker.scan()
            context.classes.values
                    .forEach { critterClass -> critterClass.build(outputDirectory) }
        } catch (e: Throwable) {
            e.printStackTrace(System.out)
        }

    }
}