package dev.morphia.critter.kotlin.extensions

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

fun KSType.fullyQualified(): TypeName {
    var propertyType = (toTypeName()
        .copy(nullable = isMarkedNullable) as ClassName).let { type ->
        if (arguments.isNotEmpty()) {
            type.parameterizedBy(arguments.map { it.toTypeName() })
        } else type
    }
    return propertyType
}