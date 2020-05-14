package com.antwerkz.critter

import com.antwerkz.critter.kotlin.isContainer
import com.antwerkz.critter.kotlin.isNumeric
import com.antwerkz.critter.kotlin.isText
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.query.experimental.filters.Filter
import dev.morphia.query.experimental.filters.Filters
import dev.morphia.query.experimental.filters.RegexFilter
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
            .toMap()
            .toSortedMap()


    @ExperimentalStdlibApi
    fun filters() = Filters::class.functions
            .filter { typeOf<Filter>().isSupertypeOf(it.returnType) }
            .filter { it.name !in setOf("and", "or", "nor", "expr", "where", "uniqueDocs", "text", "comment") }

    fun handlers(field: CritterField, target: JavaClassSource) {
        val handlers = TreeSet(Handler.common())
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
            it.handle(target, field)
        }
    }

    fun handlers(field: PropertySpec, target: TypeSpec.Builder) {
        val handlers = TreeSet(Handler.common())
        if (field.isNumeric()) {
            handlers.addAll(Handler.numerics())
        }
        if (field.isText()) {
            handlers.addAll(Handler.strings())
        }
        if (field.isContainer()) {
            handlers.addAll(Handler.containers())
        }

        handlers.forEach {
            it.handle(field, target)
        }
    }

}

@Suppress("unused", "EnumEntryName")
@ExperimentalStdlibApi
enum class Handler: OperationGenerator {
    all,
    bitsAllClear,
    bitsAllSet,
    bitsAnyClear,
    bitsAnySet,
    box,
    center,
    centerSphere,
    elemMatch {
        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
            target.addFunction(FunSpec
                    .builder(name)
                    .addParameter("values", typeOf<Filter>().asTypeName(), VARARG)
                    .addCode("""return Filters.${name}(extendPath(prefix, "${field.name}"), *values)""")
                    .build())
        }
    },
    eq,
    exists {
        override fun handle(target: JavaClassSource, field: CritterField) {
            target.addMethod("""
                public ${Filter::class.java.name} exists() {
                    return Filters.${name}(extendPath(prefix, "${field.name}"));
                } """.trimIndent())
        }

        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
            target.addFunction(FunSpec
                    .builder(name)
                    .addCode("""return Filters.${name}(extendPath(prefix, "${field.name}"))""")
                    .build())
        }
    },
    geoIntersects,
    geometry,
    geoWithin,
    gt,
    gte,
    `in` {
        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
            target.addFunction(FunSpec
                    .builder(name)
                    .addParameter(ParameterSpec.builder("value", typeOf<Iterable<Any>>().asTypeName()).build())
                    .addCode("""return Filters.`${name}`(extendPath(prefix, "${field.name}"), value)""")
                    .build())
        }

    },
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
        override fun handle(target: JavaClassSource, field: CritterField) {
            target.addMethod("""
            public ${RegexFilter::class.java.name} regex() {
                return Filters.regex(extendPath(prefix, "${field.name}"));
            } """.trimIndent())
        }

        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
            target.addFunction(FunSpec
                    .builder(name)
                    .addCode("""return Filters.${name}(extendPath(prefix, "${field.name}"))""")
                    .build())
        }
    },
    size,
    type,
    ;

    companion object {
        fun get(name: String) = valueOf(name)

        fun numerics(): List<Handler> = listOf(gt, gte, lt, lte, mod, bitsAllClear, bitsAllSet, bitsAnyClear, bitsAnySet)

        fun strings() = listOf(regex)

        fun containers() = listOf(all, elemMatch, size)

        fun geoFilters() = listOf(box, center, centerSphere, geoIntersects, geometry, geoWithin, maxDistance, minDistance, near,
                nearSphere, polygon)

        fun common() = values().toList() - numerics() - strings() - containers() - geoFilters()
    }

    open fun handle(target: JavaClassSource, field: CritterField) {
        handle(target, field, name, FilterSieve.functions, "Filters")
    }

    open fun handle(field: PropertySpec, target: TypeSpec.Builder) {
        handle(target, field, name, FilterSieve.functions, "Filters")
    }

}
