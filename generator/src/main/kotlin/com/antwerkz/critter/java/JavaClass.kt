package com.antwerkz.critter.java

import com.antwerkz.critter.CritterAnnotation
import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterContext
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.Visibility.PACKAGE
import com.antwerkz.critter.Visibility.PRIVATE
import com.antwerkz.critter.Visibility.PROTECTED
import com.antwerkz.critter.Visibility.PUBLIC
import com.antwerkz.critter.Visible
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.FieldSource
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.io.File
import org.jboss.forge.roaster.model.Visibility.PACKAGE_PRIVATE as rPACKAGE_PRIVATE
import org.jboss.forge.roaster.model.Visibility.PRIVATE as rPRIVATE
import org.jboss.forge.roaster.model.Visibility.PROTECTED as rPROTECTED
import org.jboss.forge.roaster.model.Visibility.PUBLIC as rPUBLIC

class JavaClass(context: CritterContext, val sourceFile: File,
                val sourceClass: JavaClassSource = Roaster.parse(sourceFile) as JavaClassSource)
    : CritterClass(sourceFile, sourceClass.`package`, sourceClass.name), Visible {

    init {
        this.context = context
    }

    val superClass: CritterClass? by lazy {
        context.resolve(sourceClass.`package`, sourceClass.superType)
    }

    override val annotations = mutableListOf<CritterAnnotation>()
    override val fields: List<CritterField> by lazy {
        val parent = context.resolve(name = sourceClass.superType)
        (parent?.fields ?: listOf()) + listFields(sourceClass).map { javaField ->
            CritterField(javaField.name, javaField.type.qualifiedName).apply {
                javaField.type?.typeArguments?.let {
                    if(it.isNotEmpty()) {
                        parameterizedType = it.joinToString(", ", prefix = "${javaField.type.qualifiedName}<", postfix = ">") {
                            it.qualifiedName
                        }
                    }

                    it.forEach {
                        shortParameterTypes.add(it.name)
                        fullParameterTypes.add(it.qualifiedName)
                    }
                }
                annotations += javaField.annotations.map { ann ->
                    CritterAnnotation(ann.qualifiedName, ann.values.map { it.name to it.literalValue }.toMap())
                }
            }
        }
                .sortedBy(CritterField::name)
                .toMutableList()
    }

    fun listFields(type: JavaClassSource?): List<FieldSource<JavaClassSource>> {
        return if (type != null && type.name != "java.lang.Object") {
            mutableListOf<FieldSource<JavaClassSource>>() + type.fields
        } else listOf()
    }

    init {
        annotations += sourceClass.annotations.map { ann ->
            CritterAnnotation(ann.qualifiedName, ann.values.map { Pair<String, Any>(it.name, it.stringValue) }
                    .toMap())
        }
        visibility = when (sourceClass.visibility) {
            rPUBLIC -> PUBLIC
            rPROTECTED -> PROTECTED
            rPRIVATE -> PRIVATE
            rPACKAGE_PRIVATE -> PACKAGE
            else -> PRIVATE
        }

    }

    override fun isAbstract() = sourceClass.isAbstract

    override fun lastModified(): Long {
        return Math.min(sourceFile.lastModified(), superClass?.lastModified() ?: Long.MAX_VALUE)
    }

    override fun toString(): String {
        return "JavaClass<$name>"
    }

}