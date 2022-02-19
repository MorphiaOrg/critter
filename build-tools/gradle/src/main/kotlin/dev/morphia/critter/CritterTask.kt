package dev.morphia.critter

import dev.morphia.critter.Critter.scan
import dev.morphia.critter.CritterPlugin.Companion
import org.gradle.api.file.FileTree
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CritterTask : SourceTask() {
    @Input
    lateinit var files: Set<String>

    @OutputDirectory
    var outputDirectory = File("build/generated/critter")
    @Internal
    var criteriaPackage: String? = null
    @Internal
    var outputType: String = "kotlin"
    @Internal
    var force = false

    init {
        description = "Processes files for critter."
    }

    @TaskAction
    fun generate() {
        scan(
            project.projectDir,
            files,
            criteriaPackage,
            force,
            Critter.outputType(outputType),
            outputDirectory
        )
    }
}
