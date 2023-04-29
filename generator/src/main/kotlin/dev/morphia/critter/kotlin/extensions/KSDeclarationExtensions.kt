package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import dev.morphia.critter.Critter.DEFAULT_PACKAGE
import dev.morphia.critter.snakeCase

fun KSDeclaration.name() = simpleName.asString()

fun KSDeclaration.className() = (qualifiedName ?: simpleName).asString()
fun KSDeclaration.packageName() = packageName.asString()
fun KSClassDeclaration.codecPackageName() = "${DEFAULT_PACKAGE}.${name().snakeCase()}"

fun KSDeclaration.simpleName() = simpleName.asString()