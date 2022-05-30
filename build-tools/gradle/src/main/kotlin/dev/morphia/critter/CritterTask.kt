package dev.morphia.critter

import dev.morphia.critter.Critter.generateCodecs
import dev.morphia.critter.Critter.generateCriteria
import dev.morphia.critter.Critter.scan
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CritterTask : SourceTask() {
    @Input
    lateinit var files: Set<String>

    @OutputDirectory
    lateinit var sourceOutputDirectory: File
    @OutputDirectory
    lateinit var resourceOutputDirectory: File
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
        if(!sourceOutputDirectory.isAbsolute) {
            sourceOutputDirectory = File(project.buildDir, sourceOutputDirectory.toString())
        }
//        val convention = project.convention.getPlugin(
//            JavaPluginConvention::class.java
//        )
//        val mainSourceSet: SourceSet = convention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

//        val resourceOutput =  mainSourceSet.resources.resourcesDir!!
            //File(project.buildDir, "classes/${outputType}/main")
//         File(project.buildDir,"gen-critter")
//        mainSourceSet.java.sourceDirectories.files += resourceOutput
        println("+++++++++++++++++++++ outputType = ${outputType}")
        println("resourceOutput = ${resourceOutputDirectory}")
        scan(project.projectDir, files, criteriaPackage, force, format, Critter.outputType(outputType), sourceOutputDirectory,
            resourceOutputDirectory
        )
        generateCriteria()
        generateCodecs()
    }
}
