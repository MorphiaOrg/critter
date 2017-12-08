package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterAnnotation
import com.antwerkz.critter.CritterClass
import com.antwerkz.critter.CritterField
import com.antwerkz.critter.Visibility
import com.antwerkz.kibble.model.KibbleClass
import com.antwerkz.kibble.model.KibbleProperty
import com.antwerkz.kibble.model.TypeParameter

@Suppress("UNCHECKED_CAST")
class KotlinClass(pkgName: String?, name: String, val source: KibbleClass)
    : CritterClass(pkgName, name) {

    constructor(source: KibbleClass) : this(source.file.pkgName, source.name, source)

    val list = listOf(1, 2, 3)
    override val annotations = mutableListOf<CritterAnnotation>()
    override val fields: List<CritterField> by lazy {
        listProperties(source).map { property: KibbleProperty ->
            CritterField(property.name, property.type.toString()).also { field ->
                property.type?.typeParameters?.forEach { typeParameter: TypeParameter ->
                    field.fullParameterTypes.add(typeParameter.type.toString())
                }
                field.parameterizedType = property.type.toString()
                field.annotations += property.annotations.map {
                    val fqcn = source.file.resolve(it.type).fqcn
                    CritterAnnotation(fqcn, it.arguments)
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
            current.superType?.let {
                list.addAll(listProperties(source.file.context.resolve(it)))
            }

            list += current.superTypes
                    .mapNotNull { source.file.context.resolve(it) }
                    .map { listProperties(it) }
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
            CritterAnnotation(source.file.resolve(it.type).fqcn, it.arguments)
        }
    }

    override fun isAbstract() = source.isAbstract()

    override fun isEnum() = source.isEnum()

    override fun lastModified() = (listOf(source.superType) + source.superTypes)
            .filterNotNull()
            .map { source.file.context.resolve(it)?.file?.sourceTimestamp ?: -1L }
            .max()
            ?: 0L

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }
}

