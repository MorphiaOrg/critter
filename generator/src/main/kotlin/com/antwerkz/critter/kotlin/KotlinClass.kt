package com.antwerkz.critter.kotlin

import com.antwerkz.kibble.isAbstract
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import kotlin.Long.Companion

@Suppress("UNCHECKED_CAST")
class KotlinClass(var context: KotlinContext, val fileSpec: FileSpec, val source: TypeSpec, val file: File) {

//    constructor(pkgName: String, source: TypeSpec, file: File) : this(pkgName, source.name ?: "", source, file)

    val name = source.name ?: ""
    val annotations = source.annotations
    val fields /*= source.propertySpecs
            .sortedBy(PropertySpec::name)
            .toMutableList()*/
     by lazy {
         listProperties()
     }

    internal fun listProperties(type: KotlinClass? = this): List<PropertySpec> {
        val list = mutableListOf<PropertySpec>()
        type?.let { current ->
            val typeSpec = current.source
            list += typeSpec.propertySpecs
            if (typeSpec.superclass != ANY) {
                list += listProperties(context.resolve(current.fileSpec.packageName, typeSpec.superclass.toString()))
            }

            list += current.source.superinterfaces
                    .map { listProperties(context.resolve(name, it.key.toString())) }
                    .flatMap { it }
        }
        return list

    }

    fun isAbstract() = source.isAbstract()

    fun isEnum() = source.isEnum

    fun lastModified(): Long? {
        val sourceMod = fileSpec.toJavaFileObject().lastModified
        val list = listOf(source.superclass) + source.superinterfaces.keys
                .filter { it != ANY }
        val max = list
                .mapNotNull { context.resolveFile(it.toString())?.lastModified() }
                .max()
        return Math.max(sourceMod, max ?: Long.MIN_VALUE)
    }

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }

}


internal fun CodeBlock.toPair(): Pair<String, String> {
    val split = toString().split("=")
    return split.take(1)[0] to split.drop(1).joinToString("=")
}

