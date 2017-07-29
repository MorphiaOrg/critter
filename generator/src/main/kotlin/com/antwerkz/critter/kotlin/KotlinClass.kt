package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterAnnotation
import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.Visibility
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleProperty
import com.antwerkz.kibble.model.KibbleType

class KotlinClass(pkgName: String?, name: String, val source: KibbleClass) : CritterClass(pkgName, name) {
    override val annotations: List<CritterAnnotation>
    override val fields: List<CritterField> by lazy {
        val list = source.properties +
                listProperties(source.superType) +
                source.superTypes.map { listProperties(it) }
                        .flatMap { it }
        list.map { property ->
            CritterField(property.name, source.file.resolve(property.type!!).fqcn).also { field ->
                property.type?.typeParameters?.forEach {
                    field.shortParameterTypes.add(it.name)
                    field.fullParameterTypes.add(it.name)
                }
            }
        }
                .sortedBy(CritterField::name)
                .toMutableList()
    }

    private fun listProperties(type: KibbleType?): List<KibbleProperty> {
        val file = source.file
        val context = file.context

        return type?.let { context.findClass(file.resolve(it.fqcn))?.properties } ?: listOf<KibbleProperty>()
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

