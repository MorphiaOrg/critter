package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterConstructor
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.CritterMethod
import com.antwerkz.critter.UpdaterBuilder
import com.antwerkz.critter.Visible
import com.antwerkz.critter.criteria.BaseCriteria
import com.antwerkz.kibble.FileSourceWriter
import com.antwerkz.kibble.StringSourceWriter
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleFile
import com.antwerkz.kibble.model.KibbleImport
import com.antwerkz.kibble.model.KibbleType
import com.antwerkz.kibble.model.Visibility.INTERNAL
import com.antwerkz.kibble.model.Visibility.PRIVATE
import com.antwerkz.kibble.model.Visibility.PROTECTED
import com.antwerkz.kibble.model.Visibility.PUBLIC
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.query.Query
import java.io.File

class KotlinClass(context: CritterContext, val source: KibbleClass) :
        CritterClass(context) {

    init {
        val sourceFile = source.kibbleFile.sourcePath?.let(::File)
        lastModified = Math.min(
                sourceFile?.lastModified() ?: 0,
                context[source.superType?.qualifiedName]?.lastModified ?: 0)

        isEmbedded = hasAnnotation(Embedded::class.java)
        fields = source.properties
                .map { f -> KotlinField(context, f) }
                .sortedBy { f -> f.name }
                .toMutableList()
        val superClass = context[source.superType?.qualifiedName]
        if (superClass != null) {
            fields.addAll(superClass.fields)
        }
    }

    override fun hasAnnotation(aClass: Class<out Annotation>) = source.hasAnnotation(aClass)

    override fun getName() = source.name
    override fun setName(name: String): CritterClass {
        source.name = name
        return this
    }

    override fun getPackage() = source.getPackage()
    override fun setPackage(name: String?): CritterClass {
        source.setPackage(name)
        return this
    }

    override fun getSuperType() = source.superType.toString()
    override fun setSuperType(name: String): CritterClass {
        source.superType = KibbleType.from(name)
        return this
    }

    override fun isPublic() = source.isPublic()
    override fun setPublic(): CritterClass {
        source.visibility = PUBLIC
        return this
    }

    override fun isPrivate() = source.isPrivate()
    override fun setPrivate(): CritterClass {
        source.visibility = PRIVATE
        return this
    }

    override fun isProtected() = source.isProtected()
    override fun setProtected(): CritterClass {
        source.visibility = PROTECTED
        return this
    }

    override fun isInternal() = source.isInternal()
    override fun setInternal(): CritterClass {
        source.visibility = INTERNAL
        return this
    }

    override fun isPackagePrivate() = false
    override fun setPackagePrivate() = throw Visible.invalid("package private", "kotlin")

    override fun addImport(klass: Class<*>) {
        source.kibbleFile += KibbleImport(klass.name)
    }

    override fun addImport(name: String) {
        source.kibbleFile += KibbleImport(name)
    }

    override fun addNestedType(type: CritterClass): CritterClass {
        (type as KotlinClass).source.enclosingType = this.source
        return this
    }

    override fun addConstructor(): CritterConstructor {
        return if (source.constructor == null) {
            PrimaryConstructor(source.addPrimaryConstructor())
        } else {
            SecondaryConstructor(source.addSecondaryConstructor())
        }
    }

    override fun addField(name: String, type: String): CritterField {
        val property = source.addProperty(name, type)
        property.ctorParam = true
        return KotlinField(context, property)
    }

    override fun addMethod(): CritterMethod {
        val function = source.addFunction()
        source += function
        return KotlinMethod(function)
    }

    override fun createClass(pkgName: String?, name: String): KotlinClass {
        return KotlinClass(context, source.addClass(name))
    }

    override fun buildCriteria(directory: File) {
        val kibbleFile = KibbleFile(getName() + "Criteria.kt", getPackage() + ".criteria")
        val criteriaClass = KotlinClass(context, kibbleFile.addClass(getName() + "Criteria"))

        val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".kt")

        if (context.isForce || !outputFile.exists() || outputFile.lastModified() > lastModified) {
            if (!hasAnnotation(Embedded::class.java)) {
                criteriaClass.setSuperType(BaseCriteria::class.java.name + "<" + qualifiedName + ">")
                criteriaClass.addConstructor()
                        .setPublic()
                        .setBody(java.lang.String.format("super(ds, %s.class);", getName()))
                        .addParameter(Datastore::class.java, "ds")
            } else {
                criteriaClass.addField("query", "Query<${getName()}>")
                        .setPrivate()
                criteriaClass.addField("prefix", "String")
                        .setPrivate()

                criteriaClass.addImport(Query::class.java)
                val ctor = criteriaClass.addConstructor()
                        .setPublic()
                        .setBody("""this.query = query;
this.prefix = prefix + ".";""")
            }

            fields.forEach { it.build(this, criteriaClass) }
            if (!hasAnnotation(Embedded::class.java)) {
                UpdaterBuilder(this, criteriaClass)
            }

            generate(criteriaClass.source.kibbleFile, outputFile)
        }
    }

    override fun buildDescriptor(directory: File) {
        val kibbleFile = KibbleFile(getName() + "Descriptor.kt", getPackage() + ".criteria")
        val descriptorClass = KotlinClass(context, kibbleFile.addClass(getName() + "Descriptor"))

        val outputFile = kibbleFile.outputFile(directory)
        if (context.isForce || outputFile.lastModified() < lastModified) {
            fields.forEach { field ->
                descriptorClass.addField(field.name, String::class.java.name)
                        .setPublic()
                        .setStatic()
                        .setFinal()
                        .setStringLiteralInitializer(field.mappedName())
            }

            generate(descriptorClass.source.kibbleFile, outputFile)
        }
    }

    private fun generate(kibbleFile: KibbleFile, file: File) {
        file.parentFile.mkdirs()
        FileSourceWriter(file). use {
            kibbleFile.toSource(it)
        }
    }

    override fun toSource(): String {
        val writer = StringSourceWriter()
        source.toSource(writer)
        return writer.toString()
    }
}

