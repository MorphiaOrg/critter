package com.antwerkz.critter.java

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterConstructor
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.CritterMethod
import com.antwerkz.critter.JavaUpdaterBuilder
import com.antwerkz.critter.Visible
import com.antwerkz.critter.criteria.BaseCriteria
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.query.Query
import java.io.File
import java.io.PrintWriter

class JavaClass(context: CritterContext, val sourceClass: JavaClassSource = Roaster.create(JavaClassSource::class.java))
    : CritterClass(context) {

    constructor(context: CritterContext, sourceFile: File) : this(context, Roaster.parse(sourceFile) as JavaClassSource) {
        val superClass = context.resolve(sourceClass.`package`, sourceClass.superType)
        lastModified = Math.min(
                sourceFile.lastModified(),
                superClass?.lastModified ?: 0)

        isEmbedded = hasAnnotation(Embedded::class.java)
        fields = sourceClass.fields
                .filter { f -> !f.isStatic }
                .map { f -> JavaField(context, f) }
                .sortedBy { f -> f.name }
                .toMutableList()
        if (superClass != null) {
            fields.addAll(superClass.fields)
        }
    }

    override fun isInternal() = false
    override fun setInternal() = throw Visible.invalid("internal", "java")

    override fun isPackagePrivate() = sourceClass.isPackagePrivate
    override fun setPackagePrivate(): CritterClass {
        sourceClass.setPackagePrivate()
        return this
    }

    override fun isPublic() = sourceClass.isPublic
    override fun setPublic(): CritterClass {
        sourceClass.setPublic()
        return this
    }

    override fun isPrivate() = sourceClass.isPrivate
    override fun setPrivate(): CritterClass {
        sourceClass.setPrivate()
        return this
    }

    override fun isProtected() = sourceClass.isProtected
    override fun setProtected(): CritterClass {
        sourceClass.setProtected()
        return this
    }

    override fun getName(): String = sourceClass.name
    override fun setName(name: String): CritterClass {
        sourceClass.name = name
        return this
    }

    override fun getPackage(): String? = sourceClass.`package`
    override fun setPackage(name: String?): CritterClass {
        sourceClass.`package` = name
        return this
    }

    override fun getSuperType(): String? = sourceClass.superType
    override fun setSuperType(name: String): CritterClass {
        sourceClass.superType = name
        return this
    }

    override fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return sourceClass.hasAnnotation(aClass)
    }

    override fun createClass(pkgName: String?, name: String): CritterClass {
        return JavaClass(context)
                .setPackage(pkgName)
                .setName(name)
    }

    override fun addNestedType(type: CritterClass): CritterClass {
        sourceClass.addNestedType((type as JavaClass).sourceClass)
        return this
    }

    override fun addImport(klass: Class<*>) {
        sourceClass.addImport(klass)
    }

    override fun addImport(name: String) {
        sourceClass.addImport(name)
    }

    override fun addConstructor(): CritterConstructor {
        return JavaConstructor(sourceClass.addMethod())
    }

    override fun addField(name: String, type: String): CritterField {
        val source = sourceClass.addField()
                .setName(name)
                .setType(type)
        return JavaField(context, source)
    }

    override fun addMethod(): CritterMethod {
        return JavaMethod(sourceClass.addMethod())
    }

    override fun toString(): String {
        return "${getPackage()}.${getName()}"
    }

    override fun buildDescriptor(directory: File) {
        val descriptorClass = createClass(getPackage() + ".criteria", getName() + "Descriptor")

        val outputFile = File(directory, descriptorClass.qualifiedName.replace('.', '/') + ".java")
        if (context.force || outputFile.lastModified() < lastModified) {
            fields.forEach { field ->
                descriptorClass.addField(field.name, String::class.java.name)
                        .setPublic()
                        .setStatic()
                        .setFinal()
                        .setStringLiteralInitializer(field.mappedName())
            }

            generate(descriptorClass, outputFile)
        }
    }

    override fun buildCriteria(directory: File) {
        val criteriaClass = createClass(getPackage() + ".criteria", getName() + "Criteria")

        val outputFile = File(directory, criteriaClass.qualifiedName.replace('.', '/') + ".java")
        if (context.force || !outputFile.exists() || outputFile.lastModified() > lastModified) {
            if (!hasAnnotation(Embedded::class.java)) {
                criteriaClass.setSuperType(BaseCriteria::class.java.name + "<" + qualifiedName + ">")
                criteriaClass.addConstructor()
                        .setPublic()
                        .setBody(java.lang.String.format("super(ds, %s.class);", getName()))
                        .addParameter(Datastore::class.java, "ds")
            } else {
                criteriaClass.addField("query", Query::class.java.name)
                        .setPrivate()
                criteriaClass.addField("prefix", "String")
                        .setPrivate()

                val method = criteriaClass.addConstructor()
                        .setPublic()
                        .setBody("""this.query = query;
this.prefix = prefix + ".";""")
                method.addParameter(Query::class.java, "query")
                method.addParameter(String::class.java, "prefix")
            }

            fields.forEach { it.build(this, criteriaClass) }
            if (!hasAnnotation(Embedded::class.java)) {
                JavaUpdaterBuilder(this, criteriaClass)
            }

            generate(criteriaClass, outputFile)
        }
    }
    private fun generate(criteriaClass: CritterClass, file: File) {
        file.parentFile.mkdirs()
        PrintWriter(file).use { writer -> writer.println(criteriaClass.toSource()) }
    }


    override fun toSource(): String {
        return sourceClass.toString()
    }
}

