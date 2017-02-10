package com.antwerkz.critter

import com.antwerkz.critter.criteria.BaseCriteria
import org.jboss.forge.roaster.model.Visibility
import org.jboss.forge.roaster.model.source.VisibilityScopedSource
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.query.Query
import java.io.File
import java.io.PrintWriter
import java.lang.String.format
import java.util.logging.Level
import java.util.logging.Logger

abstract class CritterClass(var context: CritterContext) {
    companion object {
        private val LOG = Logger.getLogger(CritterClass::class.java.name)
    }

    val qualifiedName: String by lazy {
        "${getPackage()}.${getName()}"
    }

    lateinit var fields: MutableList<CritterField>

    var isEmbedded: Boolean = false

    var lastModified: Long = Long.MIN_VALUE

    abstract fun hasAnnotation(aClass: Class<out Annotation>): Boolean

    abstract fun getName(): String
    abstract fun setName(name: String): CritterClass

    abstract fun getPackage(): String
    abstract fun setPackage(name: String): CritterClass

    abstract fun getSuperType(): String
    abstract fun setSuperType(name: String): CritterClass

    abstract fun isPublic(): Boolean

    abstract fun setPublic(): CritterClass

    abstract fun isPrivate(): Boolean

    abstract fun setPrivate(): CritterClass

    abstract fun isProtected(): Boolean

    abstract fun setProtected(): CritterClass

    fun build(directory: File) {
        try {
            if (hasAnnotation(Entity::class.java) || hasAnnotation(Embedded::class.java)) {
                buildCriteria(directory)
                buildDescriptor(directory)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LOG.log(Level.SEVERE, format("Failed to generate criteria class for %s: %s", getName(), e.message), e)
        }

    }

    private fun buildDescriptor(directory: File) {
        val descriptorClass = createClass(getPackage() + ".criteria", getName() + "Descriptor")

        val outputFile = File(directory, descriptorClass.qualifiedName.replace('.', '/') + ".java")
        if (context.isForce || outputFile.lastModified() < lastModified) {
            fields.forEach { field ->
                descriptorClass.addField()
                        .setPublic()
                        .setStatic()
                        .setFinal()
                        .setType(String::class.java)
                        .setName(field.name)
                        .setStringLiteralInitializer(field.mappedName())
            }

            generate(descriptorClass, outputFile)
        }
    }

    private fun buildCriteria(directory: File) {
        val criteriaClass = createClass(getPackage() + ".criteria", getName() + "Criteria")

        val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
        if (context.isForce || !outputFile.exists() || outputFile.lastModified() > lastModified) {
            if (!hasAnnotation(Embedded::class.java)) {
                criteriaClass.setSuperType(BaseCriteria::class.java.name + "<" + qualifiedName + ">")
                criteriaClass.addMethod()
                        .setPublic()
                        .setName(getName())
                        .setBody(format("super(ds, %s.class);", getName()))
                        .setConstructor(true)
                        .addParameter(Datastore::class.java, "ds")
            } else {
                criteriaClass.addField()
                        .setPrivate()
                        .setType(Query::class.java)
                        .setName("query")
                criteriaClass.addField()
                        .setPrivate()
                        .setType("String")
                        .setName("prefix")

                val method = criteriaClass.addMethod()
                        .setPublic()
                        .setName(getName())
                        .setBody("this.query = query;\nthis.prefix = prefix + \".\";")
                        .setConstructor(true)
                method.addParameter(Query::class.java, "query")
                method.addParameter(String::class.java, "prefix")
            }

            fields.forEach { it.build(this, criteriaClass) }
            if (!hasAnnotation(Embedded::class.java)) {
                UpdaterBuilder(this, criteriaClass)
            }

            generate(criteriaClass, outputFile)
        }
    }

    abstract fun createClass(pkgName: String = getPackage(), name: String): CritterClass

    private fun generate(criteriaClass: CritterClass, file: File) {
        file.parentFile.mkdirs()
        PrintWriter(file).use { writer -> writer.println(criteriaClass.toSource()) }
    }

    abstract fun toSource(): String

    abstract fun addImport(klass: Class<*>)

    abstract fun addImport(name: String)

    abstract fun addNestedType(type: CritterClass): CritterClass

    abstract fun addField(): CritterField

    abstract fun addMethod(): CritterMethod
}
