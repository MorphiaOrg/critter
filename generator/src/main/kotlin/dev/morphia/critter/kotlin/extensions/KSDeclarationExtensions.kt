package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.hasAnnotation
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import dev.morphia.annotations.PostLoad
import dev.morphia.annotations.PostPersist
import dev.morphia.annotations.PreLoad
import dev.morphia.annotations.PrePersist
import dev.morphia.critter.kotlin.className

fun KSDeclaration.name() = simpleName.asString()

fun KSDeclaration.className() = (qualifiedName ?: simpleName).asString()
fun KSDeclaration.packageName() = packageName.asString()
fun KSDeclaration.simpleName() = simpleName.asString()