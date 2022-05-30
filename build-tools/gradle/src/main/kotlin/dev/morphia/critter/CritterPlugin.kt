package dev.morphia.critter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import java.io.File

class CritterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        val mainSourceSet = project.convention.getPlugin(JavaPluginConvention::class.java)
            .sourceSets.getByName("main")
        val directories = mainSourceSet.allJava.sourceDirectories.files
        val outputDirectory = File("${project.buildDir}/generated/critter/")
        val resourceOutputDirectory = File("${project.buildDir}/generated/resources/critter/")
        outputDirectory.mkdirs()

        mainSourceSet.java.srcDirs(mainSourceSet.java.srcDirs + setOf(outputDirectory))
        mainSourceSet.resources.srcDirs(mainSourceSet.resources.srcDirs + setOf(resourceOutputDirectory))

        project.tasks.create(NAME, CritterTask::class.java) { task ->
            val rootPath = project.rootDir.absolutePath + "/"
            task.files = directories.map { it.toPath().toString().removePrefix(rootPath) }.toSet()
            task.source(task.files)
            task.sourceOutputDirectory = outputDirectory
            task.resourceOutputDirectory = resourceOutputDirectory

            project.getTasksByName("processResources", false)
                .first()
                .dependsOn(task)
        }

        for(task in arrayOf("compileJava", "compileKotlin")) {
            try {
                project.tasks.getByName(task).dependsOn(NAME)
            } catch (_: Exception) {
                project.logger.info("${NAME}:  $task not found.  Not configuring.")
            }
        }
    }

    companion object {
        const val NAME = "critter"
    }
}
