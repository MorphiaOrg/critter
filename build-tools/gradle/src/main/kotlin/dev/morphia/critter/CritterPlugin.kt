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
        val outputDirectory = File("${project.buildDir}/generated/critter/")
        outputDirectory.mkdirs()

        mainSourceSet.java.srcDirs(mainSourceSet.java.srcDirs + setOf(outputDirectory))

        project.tasks.create(NAME, CritterTask::class.java) { task ->
            val directories = mainSourceSet.allJava.sourceDirectories.files
            val rootPath = project.rootDir.absolutePath + "/"
            task.files = directories.map { it.toPath().toString().removePrefix(rootPath) }.toSet()
            task.source(task.files)
            task.outputDirectory = outputDirectory
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
