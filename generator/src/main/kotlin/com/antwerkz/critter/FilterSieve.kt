package com.antwerkz.critter

import com.antwerkz.critter.Handler.Companion.generics
import dev.morphia.query.experimental.filters.Filter
import dev.morphia.query.experimental.filters.Filters
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.util.TreeSet
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
object FilterSieve {
    internal val functions = filters()
            .map { it.name to it }
            .toMap().toSortedMap()


    @ExperimentalStdlibApi
    fun filters() = Filters::class.functions
            .filter { typeOf<Filter>().isSupertypeOf(it.returnType) }
            .filter { it.name !in setOf("and", "or", "nor", "expr", "where", "uniqueDocs", "text", "comment") }

    fun handlers(field: CritterField, target: JavaClassSource) {
        val handlers = TreeSet(generics())
        if (field.isNumeric()) {
            handlers.addAll(Handler.numerics())
        }
        if (field.isText()) {
            handlers.addAll(Handler.strings())
        }
        if (field.isContainer()) {
            handlers.addAll(Handler.containers())
        }
        if (field.isGeoCompatible()) {
            handlers.addAll(Handler.geoFilters())
        }

        handlers.forEach {
            it.java(field, target)
        }
    }
}

@ExperimentalStdlibApi
enum class Handler {
    all,
    bitsAllClear,
    bitsAllSet,
    bitsAnyClear,
    bitsAnySet,
    box {
        override fun java(field: CritterField, target: JavaClassSource) {
            target.addMethod("""
                public static Filter exists() {
                    return Filters.exists("${field.name}");
                } """.trimIndent())
        }
    },
    center,
    centerSphere,
    elemMatch,
    eq,
    exists {
        override fun java(field: CritterField, target: JavaClassSource) {
            target.addMethod("""
                public static Filter exists() {
                    return Filters.exists("${field.name}");
                } """.trimIndent())
        }
    },
    geoIntersects,
    geometry,
    geoWithin,
    gt,
    gte,
    `in`,
    jsonSchema,
    lt,
    lte,
    maxDistance,
    minDistance,
    mod,
    ne,
    near,
    nearSphere,
    nin,
    polygon,
    regex {
        override fun java(field: CritterField, target: JavaClassSource) {
            target.addMethod("""
            public RegexFilter regex() {
                return Filters.regex("${field.name}");
            } """.trimIndent())

        }
    },
    size,
    type,
    ;

    companion object {
        fun get(name: String): Handler = valueOf(name.toUpperCase())

        fun numerics(): List<Handler> = listOf(gt, gte, lt, lte, mod, bitsAllClear, bitsAllSet, bitsAnyClear, bitsAnySet)

        fun strings() = listOf(regex)

        fun containers() = listOf(all, elemMatch, size)

        fun geoFilters()  = listOf(box, center, centerSphere, geoIntersects, geometry, geoWithin, maxDistance, minDistance, near,
                nearSphere, polygon)

        fun generics() = values().toList() - numerics() - strings() - containers() - geoFilters()
    }

    open fun java(field: CritterField, target: JavaClassSource) {
        defaultCall(target, field)
    }

    private fun defaultCall(target: JavaClassSource, field: CritterField) {
        val kFunction1 = FilterSieve.functions[name]
        val kFunction = if (kFunction1 != null) {
            kFunction1
        } else {
            TODO("no handler for ${name}")
        }
        if (kFunction.parameters[0].type.javaType != String::class.java) {
            throw UnsupportedOperationException("${kFunction} needs a custom implementation")
        }
        val params = kFunction.parameters.drop(1).map {
            "final ${it.type.javaType.typeName} ${it.name}"
        }
                .joinToString(", ")
        val args = kFunction.parameters.drop(1).map { it.name }
                .joinToString(", ")

        val returnType = kFunction.returnType.javaType.typeName
        target.addMethod("""
            public ${returnType} ${kFunction.name}(${params}) {
                return Filters.${kFunction.name}("${field.name}", ${args});
            } """.trimIndent())
    }

    private fun basicCall(target: JavaClassSource, field: CritterField) {
        val kFunction = FilterSieve.functions[name] ?: TODO("no handler for ${name}")
        if (kFunction.parameters.size != 2) throw UnsupportedOperationException("${name} needs a custom implementation")
        val second = kFunction.parameters[1].type.javaType.typeName
        val returnType = kFunction.returnType.javaType.typeName
        target.addMethod("""
                    public ${returnType} ${kFunction.name}(final ${second} __value) {
                           return Filters.${kFunction.name}("${field.name}", __value);
                    } """.trimIndent())
    }
}
