package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.symbol.KSAnnotated

fun KSAnnotated.morphiaAnnotations() = annotations
    .filter { it.annotationType.packageName().startsWith("dev.morphia") }