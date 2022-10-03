package dev.morphia.critter.kotlin

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

@Suppress("UNCHECKED_CAST")
class KotlinClass(var context: KotlinContext, val fileSpec: FileSpec, val source: TypeSpec, val file: File) {
    val name = source.name ?: ""
    val annotations = source.annotationSpecs
    val fields by lazy { listProperties() }

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

    fun lastModified(): Long {
        val sourceMod = fileSpec.toJavaFileObject().lastModified
        val list = listOf(source.superclass) + source.superinterfaces.keys
                .filter { it != ANY }
        val longs = list.mapNotNull { context.resolveFile(it.toString())?.lastModified() }
        val max = if(longs.isEmpty()) null else longs.max()
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

fun TypeSpec.isAbstract() = KModifier.ABSTRACT in modifiers
