package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.hasAnnotation
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import dev.morphia.annotations.Id
import dev.morphia.annotations.PostLoad
import dev.morphia.annotations.PostPersist
import dev.morphia.annotations.PreLoad
import dev.morphia.annotations.PrePersist

fun KSClassDeclaration.activeProperties() = getAllProperties()
    .filter { it.isNotTransient() }

fun KSClassDeclaration.toTypeName(): TypeName {
    return ClassName(packageName.asString(), simpleName.asString())
}

fun KSClassDeclaration.functions(annotation: Class<out Annotation>): List<KSFunctionDeclaration> {
    return getAllFunctions()
        .filter { it.hasAnnotation(annotation.name) }
        .toList()
}

fun KSClassDeclaration.bestConstructor(): KSFunctionDeclaration? {
    val ctors = getConstructors()
    if(hasLifecycleEvents()) {
        val method = ctors.firstOrNull { it.parameters.isEmpty() }
        return method ?: throw IllegalStateException("A type with lifecycle events must have a no-arg constructor")
    }
    val propertyMap = this.activeProperties()
        .map { it.name() to it.type }
        .toMap()

    val all = ctors
        .filter {
            it.parameters.all { param ->
                val type = propertyMap[param.name?.asString()]
                val same = type?.className() == param.type.className()
                same
            }
        }
        .toList()
    val matches = all
        .sortedBy { it.parameters.size }
        .reversed()

    return matches.firstOrNull()
}

fun KSClassDeclaration.hasLifecycleEvents(): Boolean {
    return functions(PreLoad::class.java).isNotEmpty()
        || functions(PostLoad::class.java).isNotEmpty()
        || functions(PrePersist::class.java).isNotEmpty()
        || functions(PostPersist::class.java).isNotEmpty()
}

fun KSClassDeclaration.className() = qualifiedName?.asString() ?: this.simpleName.asString()
fun KSClassDeclaration.interfaces() =
    declarations.filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == INTERFACE }