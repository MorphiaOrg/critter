package dev.morphia.critter.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import dev.morphia.critter.CritterAnnotation
import dev.morphia.critter.CritterMethod
import dev.morphia.critter.CritterParameter
import dev.morphia.critter.CritterType
import nullable

private fun KSAnnotation.toCritter(): CritterAnnotation {
    val annotation = this

    fun toCritter(value: Any) =
        when (value) {
            is KSAnnotation -> value.toCritter()
            is KSNode -> TODO(value.javaClass.name)
            else -> value
        }

    val map = this.arguments.map {
        (it.name?.asString() ?: "") to toCritter(it.value ?: TODO("not sure how to handle this case"))
    }.toMap()

    return object: CritterAnnotation(this.annotationType.toCritter(), map) {
        override fun literalValue(name: String): Any? {
            return annotation.arguments
                .firstOrNull { it.name?.asString() == name }
                ?.value
        }
        override fun annotationArrayValue(): List<CritterAnnotation> {
            TODO("Not yet implemented")
        }
        override fun annotationValue(name: String): CritterAnnotation {
            TODO("Not yet implemented")
        }
    }
}

private fun PropertySpec.isTransient() =
    hasAnnotation(Transient::class.java) ||
        hasAnnotation(dev.morphia.annotations.Transient::class.java)

fun KSFunctionDeclaration.toCritter(): CritterMethod {
    return CritterMethod(this.name(), parameters
        .map { param -> param.toCritter() }, returnType?.toCritter())
}

/*
fun PropertySpec.toCritter(): CritterProperty {
    return CritterProperty(name, type.toCritter(), annotations.map { it.toCritter() }, !mutable, initializer.toString())
}
*/

private fun KSTypeReference?.toCritter(): CritterType {
    return when {
        this == null -> CritterType(UNIT.canonicalName, nullable = false)
        else -> CritterType(className(), nullable = nullable())
    }
}

fun KSDeclaration.className() = (qualifiedName ?: simpleName).asString()
fun KSDeclaration.packageName() = packageName.asString()
fun KSDeclaration.simpleName() = simpleName.asString()
fun KSTypeReference.className() = resolve().declaration.className()
internal fun KSTypeReference.packageName() = resolve().declaration.packageName()

internal fun KSTypeReference.simpleName() = resolve().declaration.simpleName()

fun KSValueParameter.toCritter(): CritterParameter {
    return CritterParameter(name?.asString() ?: "", type.toCritter(), false, annotations.map { it.toCritter() }.toList())
}

internal fun CodeBlock.toPair(): Pair<String, String> {
    val split = toString().split("=")
    return split.take(1)[0] to split.drop(1).joinToString("=")
}

fun TypeSpec.isAbstract() = KModifier.ABSTRACT in modifiers
