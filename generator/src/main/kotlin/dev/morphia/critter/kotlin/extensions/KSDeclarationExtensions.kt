package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.symbol.KSDeclaration

fun KSDeclaration.name() = simpleName.asString()

fun KSDeclaration.className() = (qualifiedName ?: simpleName).asString()
fun KSDeclaration.packageName() = packageName.asString()
fun KSDeclaration.simpleName() = simpleName.asString()