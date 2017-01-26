package com.antwerkz.critter

import com.antwerkz.critter.criteria.BaseCriteria
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.query.Query
import java.io.File
import java.io.PrintWriter
import java.lang.String.format
import java.util.logging.Level
import java.util.logging.Logger

class CritterClass(private val context: CritterContext, private val sourceFile: File, type: JavaType<*>) {

    var name: String? = null

    var pkgName: String? = null

    val sourceClass: JavaClassSource = type as JavaClassSource

    val isEmbedded: Boolean

    val fields: List<CritterField>

    private val lastModified: Long by lazy {
            var modified = sourceFile.lastModified()
            val superClass = context[sourceClass.superType]
            if (superClass != null) {
                modified = Math.min(modified, superClass.lastModified)
            }
            modified
        }

    init {
        name = sourceClass.name
        isEmbedded = sourceClass.hasAnnotation(Embedded::class.java)
        pkgName = sourceClass.`package` + ".criteria"
        fields = sourceClass.fields
                .filter { f -> !f.isStatic }
                .map { f -> CritterField(context, f) }
                .sortedBy { f -> f.name }
                .toMutableList()
        val superClass = context[sourceClass.superType]
        if (superClass != null) {
            fields.addAll(superClass.fields)
        }
    }

    fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return sourceClass.hasAnnotation(aClass)
    }

    fun build(directory: File) {
        try {
            if (hasAnnotation(Entity::class.java) || hasAnnotation(Embedded::class.java)) {
                buildCriteria(directory)
                buildDescriptor(directory)
            }
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, format("Failed to generate criteria class for %s: %s", name, e.message), e)
        }

    }

    private fun buildDescriptor(directory: File) {
        val descriptorClass = Roaster.create(JavaClassSource::class.java)
        descriptorClass.setPackage(pkgName).name = sourceClass.name + "Descriptor"

        val outputFile = File(directory, descriptorClass.qualifiedName.replace('.', '/') + ".java")
        if (context.isForce || outputFile.lastModified() < lastModified) {
            fields.forEach { field ->
                descriptorClass.addField()
                        .setPublic()
                        .setStatic(true)
                        .setFinal(true)
                        .setType(String::class.java)
                        .setName(field.name).stringInitializer = field.mappedName()
            }

            generate(descriptorClass, outputFile)
        }
    }

    private fun buildCriteria(directory: File) {
        val criteriaClass = Roaster.create(JavaClassSource::class.java)
        criteriaClass.setPackage(pkgName).name = sourceClass.name + "Criteria"

        val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
        if (context.isForce || outputFile.lastModified() < lastModified) {
            if (!sourceClass.hasAnnotation(Embedded::class.java)) {
                criteriaClass.superType = BaseCriteria::class.java.name + "<" + sourceClass.qualifiedName + ">"
                criteriaClass.addMethod()
                        .setPublic()
                        .setName(name)
                        .setBody(format("super(ds, %s.class);", sourceClass.name))
                        .setConstructor(true)
                        .addParameter(Datastore::class.java, "ds")
            } else {
                criteriaClass.addField()
                        .setPrivate()
                        .setType(Query::class.java).name = "query"
                criteriaClass.addField()
                        .setPrivate()
                        .setType("String").name = "prefix"

                val method = criteriaClass.addMethod()
                        .setPublic()
                        .setName(name)
                        .setBody("this.query = query;\nthis.prefix = prefix + \".\";")
                        .setConstructor(true)
                method.addParameter(Query::class.java, "query")
                method.addParameter(String::class.java, "prefix")
            }

            fields.forEach { it.build(this, criteriaClass) }
            if (!sourceClass.hasAnnotation(Embedded::class.java)) {
                UpdaterBuilder(this, criteriaClass)
            }

            generate(criteriaClass, outputFile)
        }
    }

    private fun generate(criteriaClass: JavaClassSource, file: File) {
        file.parentFile.mkdirs()
        PrintWriter(file).use { writer -> writer.println(criteriaClass.toString()) }

    }

/*
    fun getFields(): List<CritterField> {
        if (fields == null) {
        }
        return fields
    }
*/

    override fun toString(): String {
        return pkgName + "." + name
    }

    companion object {
        private val LOG = Logger.getLogger(CritterClass::class.java.name)
    }
}
