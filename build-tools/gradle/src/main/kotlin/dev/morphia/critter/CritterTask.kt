package dev.morphia.critter

import dev.morphia.critter.Critter.scan
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CritterTask : SourceTask() {
    @Input
    lateinit var files: Set<String>

    @OutputDirectory
    var outputDirectory = File("build/generated/critter")

    @Input
    var criteriaPackage: String? = null

    @Input
    var outputType: String = "kotlin"

    @Input
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
