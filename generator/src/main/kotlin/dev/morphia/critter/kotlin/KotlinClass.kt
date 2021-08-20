package dev.morphia.critter.kotlin

import com.antwerkz.kibble.properties
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import dev.morphia.critter.CritterAnnotation
import dev.morphia.critter.CritterClass
import dev.morphia.critter.CritterMethod
import dev.morphia.critter.CritterParameter
import dev.morphia.critter.CritterProperty
import dev.morphia.critter.CritterType
import java.io.File

@Suppress("UNCHECKED_CAST")
class KotlinClass(var context: KotlinContext, val fileSpec: FileSpec, val source: TypeSpec, file: File) :
    CritterClass(source.name ?: "", fileSpec.packageName, file) {

    val annotations = source.annotationSpecs
    val fields by lazy { listProperties() }
    val qualifiedName: String by lazy {
        pkgName?.let { "$pkgName.$name" } ?: name
    }

    private fun listProperties(type: KotlinClass? = this): List<PropertySpec> {
        val list = mutableListOf<PropertySpec>()
        type?.let { current ->
            val typeSpec = current.source
            list += typeSpec.propertySpecs
            if (typeSpec.superclass != ANY) {
                list += listProperties(context.resolve(current.fileSpec.packageName, typeSpec.superclass.toString()))
            }

            list += current.source.superinterfaces
                .map { listProperties(context.resolve(name, it.key.toString())) }
                .flatten()
        }

        return list
    }

    fun isAbstract() = source.isAbstract()
    fun isEnum() = source.isEnum
    fun lastModified(): Long {
        val sourceMod = file.lastModified()
        val list = listOf(source.superclass) + source.superinterfaces.keys
            .filter { it != ANY }
        val max = list
            .mapNotNull { context.resolveFile(it.toString())?.lastModified() }
            .maxOrNull()
        return sourceMod.coerceAtLeast(max ?: Long.MIN_VALUE)
    }

    override fun toString(): String {
        return "KotlinClass(${source.name})"
    }

    fun functions(annotation: Class<out Annotation>): List<CritterMethod> {
        return source.funSpecs
            .filter { it.annotations.contains(AnnotationSpec.builder(annotation).build()) }
            .map { it.toCritter() }
    }
}

private fun AnnotationSpec.toCritter(): CritterAnnotation {
    return object: CritterAnnotation(this.typeName.toString()/*, this.members*/) {
        override fun literalValue(name: String): String {
            TODO("Not yet implemented")
        }
        override fun annotationArrayValue(): List<CritterAnnotation>? {
            TODO("Not yet implemented")
        }
        override fun annotationValue(name: String): CritterAnnotation? {
            TODO("Not yet implemented")
        }
    }
}

private fun PropertySpec.isTransient() =
    hasAnnotation(Transient::class.java) ||
        hasAnnotation(dev.morphia.annotations.Transient::class.java)

fun FunSpec.toCritter(): CritterMethod {
    return CritterMethod(name, parameters
        .map { param -> param.toCritter() }, returnType?.toCritter())
}

fun PropertySpec.toCritter(): CritterProperty {
    return CritterProperty(name, type.toCritter(), annotations.map { it.toCritter() }, !mutable, initializer?.toString())
}

private fun TypeName?.toCritter(): CritterType {
    return when {
        this == null -> CritterType(UNIT.canonicalName)
        this is ClassName -> {
            CritterType(this.canonicalName)
        }
        this is ParameterizedTypeName -> {
            CritterType(this.rawType.canonicalName, typeArguments.map { it.toCritter() })
        }
        else -> TODO("handle this type: ${this::class.java}")
    }
}

fun ParameterSpec.toCritter(): CritterParameter {
    TODO("Not yet implemented")
}

internal fun CodeBlock.toPair(): Pair<String, String> {
    val split = toString().split("=")
    return split.take(1)[0] to split.drop(1).joinToString("=")
}

fun TypeSpec.isAbstract() = KModifier.ABSTRACT in modifiers
