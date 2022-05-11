package dev.morphia.critter

import dev.morphia.critter.Critter.generateCodecs
import dev.morphia.critter.Critter.generateCriteria
import dev.morphia.critter.Critter.scan
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CritterTask : SourceTask() {
    @Input
    lateinit var files: Set<String>

    @OutputDirectory
    var sourceOutputDirectory = File("generated/critter")
    @OutputDirectory
    var resourceOutputDirectory = File("generated-resources/critter")
    @Input
    @Optional
    var criteriaPackage: String? = null
    @Input
    var outputType: String = "kotlin"
    @Input
    var force = false
    @Input
    var format = false

    init {
        description = "Processes files for critter."
    }

    @TaskAction
    fun generate() {
        scan(project.projectDir, files, criteriaPackage, force, format, Critter.outputType(outputType),
            File(project.buildDir, sourceOutputDirectory.toString()),
            File(project.buildDir, resourceOutputDirectory.toString()))
        generateCriteria()
        generateCodecs()
    }
}
