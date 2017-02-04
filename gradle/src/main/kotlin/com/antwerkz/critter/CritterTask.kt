package com.antwerkz.critter

import com.antwerkz.critter.java.JavaClass
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
                    val critterClass = JavaClass(context, it)
                    context.add(critterClass.getPackage(), critterClass)
                }

        context.classes.values
                .forEach { critterClass -> critterClass.build(outputDirectory!!) }
    }
}
