package com.antwerkz.critter

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import java.io.File

class CritterTask : SourceTask() {
    var extension: CritterPluginExtension? = null

    var outputDirectory: File? = null

    @TaskAction
    fun generate() {
        val context = CritterContext(extension!!.criteriaPackage, extension!!.force)
        getSource().files
                .filterNot { it.name.endsWith("Criteria.java") }
                .forEach {
                    val type: JavaType<*> = Roaster.parse(it)
                    context.add(type.getPackage(), CritterClass(context, it, type))
                }

        context.classes.values
                .forEach { critterClass -> critterClass.build(outputDirectory!!) }
    }
}
