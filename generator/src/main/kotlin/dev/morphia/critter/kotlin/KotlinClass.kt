package dev.morphia.critter.kotlin

import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec



internal fun CodeBlock.toPair(): Pair<String, String> {
    val split = toString().split("=")
    return split.take(1)[0] to split.drop(1).joinToString("=")
}

