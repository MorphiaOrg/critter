package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterAnnotation
import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.Visibility
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleProperty

class KotlinClass(pkgName: String?, name: String, val source: KibbleClass) : CritterClass(pkgName, name) {
    override val annotations: List<CritterAnnotation>
    override val fields: List<CritterField> by lazy {
        listProperties(source).map { property: KibbleProperty ->
            CritterField(property.name, property.type.toString()).also { field ->
                property.type?.typeParameters?.forEach {
                    field.shortParameterTypes.add(it.name)
                    field.fullParameterTypes.add(it.name)
                }
                field.parameterizedType = property.type.toString()
                field.fullyQualifiedType = source.file.resolve(property.type!!).fqcn
            }
        }
                .sortedBy(CritterField::name)
                .toMutableList()
    }

    private fun listProperties(type: KibbleClass?): List<KibbleProperty> {
        return type?.let { current ->
            val context = type.file.context

            val list = mutableListOf<KibbleProperty>()
            list.addAll(current.properties)
            current.superType?.let {
                list.addAll(listProperties(context.findClass(it)))
            }
            list.addAll(current.superTypes
                    .map { context.resolve(source.file, it) }
                    .filterNotNull()
                    .map { listProperties(context.findClass(it)) }
                    .flatMap { it })

/*
            current.let {
                val findClass = context.findClass(source.file.resolve(it.fqcn))
                findClass?.properties
            }
*/
            list
        } ?: listOf<KibbleProperty>()
    }

    init {
        visibility = when (source.visibility) {
            com.antwerkz.kibble.model.Visibility.PUBLIC -> Visibility.PUBLIC
            com.antwerkz.kibble.model.Visibility.PROTECTED -> Visibility.PROTECTED
            com.antwerkz.kibble.model.Visibility.PRIVATE -> Visibility.PRIVATE
            com.antwerkz.kibble.model.Visibility.INTERNAL -> Visibility.INTERNAL
            else -> Visibility.PUBLIC
        }
        annotations = source.annotations.map {
            CritterAnnotation(it.type.fqcn, it.arguments)
        }
    }

    override fun lastModified() = (listOf(source.superType) + source.superTypes)
            .mapNotNull { source.file.context.findClass(it!!)?.file?.sourceTimestamp ?: -1L }
            .max()?.toLong() ?: 0L

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }
}

