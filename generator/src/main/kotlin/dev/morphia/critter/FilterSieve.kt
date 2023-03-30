package dev.morphia.critter

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.critter.kotlin.extensions.isContainer
import dev.morphia.critter.kotlin.extensions.isNumeric
import dev.morphia.critter.kotlin.extensions.isText
import dev.morphia.query.filters.Filter
import dev.morphia.query.filters.Filters
import dev.morphia.query.filters.RegexFilter
import java.util.TreeSet
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSupertypeOf
import org.jboss.forge.roaster.model.source.JavaClassSource

object FilterSieve {
    internal val functions = filters()
        .map { it.name to it }
        .toMap()
        .toSortedMap()

    fun filters() = Filters::class.functions
        .filter { Filter::class.createType().isSupertypeOf(it.returnType) }
        .filter { it.name !in setOf("and", "or", "nor", "expr", "where", "uniqueDocs", "text", "comment") }

    fun handlers(property: CritterProperty, target: JavaClassSource) {
        val handlers = TreeSet(Handler.common())
        if (property.type.isNumeric) {
            handlers.addAll(Handler.numerics())
        }
        if (property.type.isText) {
            handlers.addAll(Handler.strings())
        }
        if (property.type.isContainer) {
            handlers.addAll(Handler.containers())
        }
        if (property.type.isGeoCompatible) {
            handlers.addAll(Handler.geoFilters())
        }
        handlers.forEach {
            it.handle(target, property)
        }
    }

    fun handlers(property: KSPropertyDeclaration, target: Builder) {
        val handlers = TreeSet(Handler.common())
        if (property.isNumeric()) {
            handlers.addAll(Handler.numerics())
        }
        if (property.isText()) {
            handlers.addAll(Handler.strings())
        }
        if (property.isContainer()) {
            handlers.addAll(Handler.containers())
        }

        handlers.forEach {
            it.handle(property, target)
        }
    }
}

@Suppress("unused", "EnumEntryName")
enum class Handler : OperationGenerator {
    all,
    bitsAllClear,
    bitsAllSet,
    bitsAnyClear,
    bitsAnySet,
    box,
    center,
    centerSphere,
    elemMatch {
        override fun handle(property: KSPropertyDeclaration, target: Builder) {
            target.addFunction(
                FunSpec
                    .builder(name)
                    .addParameter("values", Filter::class.asTypeName(), VARARG)
                    .addCode("""return Filters.${name}(path, *values)""")
                    .build()
            )
        }
    },
    eq,
    exists {
        override fun handle(target: JavaClassSource, property: CritterProperty) {
            target.addMethod(
                """
                public ${Filter::class.java.name} exists() {
                    return Filters.${name}(path);
                } """.trimIndent()
            )
        }

        override fun handle(property: KSPropertyDeclaration, target: Builder) {
            target.addFunction(
                FunSpec
                    .builder(name)
                    .addCode("""return Filters.${name}(path)""")
                    .build()
            )
        }
    },
    geoIntersects,
    geometry,
    geoWithin,
    gt,
    gte,
    `in` {
        override fun handle(property: KSPropertyDeclaration, target: Builder) {
            target.addFunction(
                FunSpec
                    .builder(name)
                    .addParameter(ParameterSpec.builder("__value", Iterable::class.asClassName().parameterizedBy(STAR)).build())
                    .addCode("""return Filters.`${name}`(path, __value)""")
                    .build()
            )
        }
    },
    jsonSchema {
        override fun handle(property: KSPropertyDeclaration, target: Builder) {}
        override fun handle(target: JavaClassSource, property: CritterProperty) {}
    },
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
        override fun handle(target: JavaClassSource, property: CritterProperty) {
            target.addMethod(
                """
            public ${RegexFilter::class.java.name} regex() {
                return Filters.regex(path);
            } """.trimIndent()
            )
        }

        override fun handle(property: KSPropertyDeclaration, target: Builder) {
            target.addFunction(
                FunSpec
                    .builder(name)
                    .addCode("""return Filters.${name}(path)""")
                    .build()
            )
        }
    },
    size,
    type,
    ;

    companion object {
        fun get(name: String) = valueOf(name)
        fun numerics(): Set<Handler> = setOf(gt, gte, lt, lte, mod, bitsAllClear, bitsAllSet, bitsAnyClear, bitsAnySet)
        fun strings() = listOf(regex)
        fun containers() = listOf(all, elemMatch, size)
        fun geoFilters() = listOf(
            box, center, centerSphere, geoIntersects, geometry, geoWithin, maxDistance, minDistance, near,
            nearSphere, polygon
        )

        fun common() = values().toList() - numerics() - strings() - containers() - geoFilters()
    }

    open fun handle(target: JavaClassSource, property: CritterProperty) {
        handle(target, property, name, FilterSieve.functions, "Filters")
    }

    open fun handle(property: KSPropertyDeclaration, target: Builder) {
        handle(target, property, name, FilterSieve.functions, "Filters")
    }
}
