package dev.morphia.critter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.COMPILE_CONFIGURATION_NAME
import org.gradle.api.plugins.JavaPluginConvention
import java.io.File

class CritterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        val extension = project.extensions.create("critter", CritterPluginExtension::class.java)

        val critterConfiguration = project.configurations.create(CRITTER_CONFIGURATION_NAME).setVisible(false)
                .setTransitive(false).setDescription("The Critter libraries to be used for this project.")
        project.configurations.getByName(COMPILE_CONFIGURATION_NAME).extendsFrom(critterConfiguration)

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { sourceSet ->
            val taskName = sourceSet.getTaskName("generate", "CritterSource")
            val task = project.tasks.create(taskName, CritterTask::class.java)
            task.description = "Processes the ${sourceSet.name} java files for critter."

            task.setSource(sourceSet.allJava.files)
            task.extension = extension

            task.conventionMapping.map("critterClasspath"
            ) { project.configurations.getByName(CRITTER_CONFIGURATION_NAME).copy().setTransitive(true) }

            val outputDirectoryName = "${project.buildDir}/generated-src/critter/${sourceSet.name}"
            val outputDirectory = File(outputDirectoryName)
            task.outputDirectory = outputDirectory
            sourceSet.java.srcDir(outputDirectory)

            project.tasks.getByName(sourceSet.compileJavaTaskName).dependsOn(taskName)
        }
    }

    companion object {
        val CRITTER_CONFIGURATION_NAME = "critter"
    }
}
