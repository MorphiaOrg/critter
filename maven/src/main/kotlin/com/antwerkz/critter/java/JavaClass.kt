package com.antwerkz.critter.java

import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.CritterMethod
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.mongodb.morphia.annotations.Embedded
import java.io.File

class JavaClass(context: CritterContext, val sourceClass: JavaClassSource = Roaster.create(JavaClassSource::class.java))
    : CritterClass(context) {

    constructor(context: CritterContext, sourceFile: File) : this(context, Roaster.parse(sourceFile) as JavaClassSource) {
        lastModified = Math.min(
                sourceFile.lastModified(),
                context[sourceClass.superType]?.lastModified ?: 0)

        isEmbedded = hasAnnotation(Embedded::class.java)
        fields = sourceClass.fields
                .filter { f -> !f.isStatic }
                .map { f -> JavaField(context, f) }
                .sortedBy { f -> f.name }
                .toMutableList()
        val superClass = context[sourceClass.superType]
        if (superClass != null) {
            fields.addAll(superClass.fields)
        }
    }

    override fun isPublic(): Boolean {
        return sourceClass.isPublic
    }

    override fun setPublic(): CritterClass {
        sourceClass.setPublic()
        return this
    }

    override fun isPrivate(): Boolean {
        return sourceClass.isPrivate
    }

    override fun setPrivate(): CritterClass {
        sourceClass.setPrivate()
        return this
    }

    override fun isProtected(): Boolean {
        return sourceClass.isProtected
    }

    override fun setProtected(): CritterClass {
        sourceClass.setProtected()
        return this
    }

    override fun getName(): String {
        return sourceClass.name
    }

    override fun setName(name: String): CritterClass {
        sourceClass.name = name
        return this
    }

    override fun getPackage(): String {
        return sourceClass.`package`
    }

    override fun setPackage(name: String): CritterClass {
        sourceClass.`package` = name
        return this
    }

    override fun getSuperType(): String {
        return sourceClass.superType
    }

    override fun setSuperType(name: String): CritterClass {
        sourceClass.superType = name
        return this
    }

    override fun hasAnnotation(aClass: Class<out Annotation>): Boolean {
        return sourceClass.hasAnnotation(aClass)
    }

    override fun createClass(pkgName: String, name: String): CritterClass {
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

    override fun addField(): CritterField {
        return JavaField(context, sourceClass.addField())
    }

    override fun addMethod(): CritterMethod {
        return JavaMethod(sourceClass.addMethod())
    }

    override fun toString(): String {
        return "${getPackage()}.${getName()}"
    }

    override fun toSource(): String {
        return sourceClass.toString()
    }
}
