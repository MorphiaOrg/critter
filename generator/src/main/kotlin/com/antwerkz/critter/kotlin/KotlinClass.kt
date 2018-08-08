package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterAnnotation
import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.Visibility
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleProperty
import com.antwerkz.kibble.model.TypeParameter
import java.io.File

@Suppress("UNCHECKED_CAST")
class KotlinClass(pkgName: String?, name: String, val source: KibbleClass, file: File)
    : CritterClass(file, pkgName, name) {

    constructor(pkgName: String, source: KibbleClass, file: File) : this(pkgName, source.name, source, file)

    override val annotations = mutableListOf<CritterAnnotation>()
    override val fields: List<CritterField> by lazy {
        listProperties(source).map { property: KibbleProperty ->
            CritterField(property.name, property.type?.externalize() ?: "").also { field ->
                property.type?.typeParameters?.forEach { typeParameter: TypeParameter ->
                    field.fullParameterTypes.add(typeParameter.type.toString())
                }
                field.parameterizedType = property.type.toString()
                field.annotations += property.annotations.map {
                    CritterAnnotation(it.type.fqcn(), it.arguments
                            .map { (it.name ?: "value") to it.value }
                            .toMap())
                }
            }
        }
                .sortedBy(CritterField::name)
                .toMutableList()
    }

    private fun listProperties(type: KibbleClass?): List<KibbleProperty> {
        val list = mutableListOf<KibbleProperty>()
        type?.let { current ->
            list.addAll(current.properties)
            current.extends?.let {
                list.addAll(listProperties(current.context.resolve(it)))
            }

            list += current.implements
                    .map { listProperties(current.context.resolve(it)) }
                    .flatMap { it }
        }
        return list

    }

    init {
        visibility = when (source.visibility) {
            com.antwerkz.kibble.model.Visibility.PUBLIC -> Visibility.PUBLIC
            com.antwerkz.kibble.model.Visibility.PROTECTED -> Visibility.PROTECTED
            com.antwerkz.kibble.model.Visibility.PRIVATE -> Visibility.PRIVATE
            com.antwerkz.kibble.model.Visibility.INTERNAL -> Visibility.INTERNAL
            else -> Visibility.PUBLIC
        }
        annotations += source.annotations.map {
            CritterAnnotation(it.type.fqcn(), it.arguments
                    .map { (it.name ?: "value") to it.value }
                    .toMap())
        }
    }

    override fun isAbstract() = source.isAbstract()

    override fun isEnum() = source.isEnum()

    override fun lastModified(): Long {
        val list = listOf(source.extends) + source.implements
        return list
                .filterNotNull()
                .map { it -> context.resolveFile(it.fqcn())
                        ?.lastModified() ?: -1L }
                .max()
                ?: 0L
    }

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }
}

