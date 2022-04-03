package dev.morphia.critter

import dev.morphia.critter.Critter.addMethods
import dev.morphia.critter.kotlin.isNumeric
import dev.morphia.critter.kotlin.isText
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.critter.kotlin.isContainer
import dev.morphia.query.updates.AddToSetOperator
import dev.morphia.query.updates.UpdateOperator
import dev.morphia.query.updates.UpdateOperators
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.util.TreeSet
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSupertypeOf

object UpdateSieve {
    internal val functions = updates()
            .map { it.name to it }
            .toMap()
            .toSortedMap()

    fun updates() = UpdateOperators::class.functions
            .filter { UpdateOperator::class.createType().isSupertypeOf(it.returnType) }
            .filter { it.name !in setOf("setOnInsert") }
            .filterNot { it.name == "set" && it.parameters.size == 1 }

    fun handlers(property: CritterProperty, target: JavaClassSource) {
        val updates = TreeSet(Updates.common())
        if (property.type.isNumeric()) {
            updates.addAll(Updates.numerics())
        }
        if (property.type.isText()) {
            updates.addAll(Updates.strings())
        }
        if (property.type.isContainer()) {
            updates.addAll(Updates.containers())
        }
        updates.forEach {
            it.handle(target, property)
        }
    }

    fun handlers(field: PropertySpec, target: TypeSpec.Builder) {
        val updates = TreeSet(Updates.common())
        if (field.isNumeric()) {
            updates.addAll(Updates.numerics())
        }
        if (field.isText()) {
            updates.addAll(Updates.strings())
        }
        if (field.isContainer()) {
            updates.addAll(Updates.containers())
        }

        updates.forEach {
            it.handle(target, field)
        }
    }
}

@Suppress("EnumEntryName")
enum class Updates : OperationGenerator {
    and,
    addToSet {
        override fun handle(target: JavaClassSource, property: CritterProperty) {
            target.addImport(AddToSetOperator::class.java)
            target.addMethods("""
            public AddToSetOperator ${name}(Object value) {
                return UpdateOperators.${name}(path, value);
            }         
            
            public AddToSetOperator ${name}(List<?> values) {
                return UpdateOperators.${name}(path, values);
            } """.trimIndent())
        }

        override fun handle(target: Builder, field: PropertySpec) {
            target.addFunction(FunSpec.builder(name)
                    .addParameter("value", Any::class.asTypeName())
                    .returns(AddToSetOperator::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(path, value)""")
                    .build())

            target.addFunction(FunSpec.builder(name)
                    .addParameter("values", List::class.asClassName().parameterizedBy(STAR))
                    .returns(AddToSetOperator::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(path, values)""")
                    .build())
        }
    },
    currentDate,
    dec {
        override fun handle(target: JavaClassSource, property: CritterProperty) {
            target.addMethod("""
                public UpdateOperator ${name}() {
                    return UpdateOperators.${name}(path);
                } """.trimIndent())
            target.addMethod("""
                public UpdateOperator ${name}(Number value) {
                    return UpdateOperators.${name}(path, value);
                } """.trimIndent())
        }

        override fun handle(target: Builder, field: PropertySpec) {
            target.addFunction(FunSpec.builder(name)
                    .addCode("""return UpdateOperators.${name}(path)""")
                    .build())
            target.addFunction(FunSpec.builder(name)
                    .addParameter("value", Number::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(path, value)""")
                    .build())
        }
    },
    inc {
        override fun handle(target: JavaClassSource, property: CritterProperty) {
            target.addMethod("""
                public UpdateOperator ${name}() {
                    return UpdateOperators.${name}(path);
                } """.trimIndent())
            target.addMethod("""
                public UpdateOperator ${name}(Number value) {
                    return UpdateOperators.${name}(path, value);
                } """.trimIndent())
        }

        override fun handle(target: Builder, field: PropertySpec) {
            target.addFunction(FunSpec.builder(name)
                    .addCode("""return UpdateOperators.${name}(path)""")
                    .build())
            target.addFunction(FunSpec.builder(name)
                    .addParameter("value", Number::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(path, value)""")
                    .build())
        }
    },
    max,
    min,
    mul,
    or,
    pop,
    pull,
    pullAll {
        override fun handle(target: JavaClassSource, property: CritterProperty) {
            target.addImport(java.util.List::class.java)
            target.addMethod("""
            public UpdateOperator ${name}(List<?> values) {
                return UpdateOperators.${name}(path, values);
            } """.trimIndent())
        }
    },
    push,
    rename,
    set,
    unset,
    xor;

    companion object {
        operator fun get(name: String) = valueOf(name)
        fun numerics(): List<Updates> = listOf(and, dec, inc, max, min, mul, or, xor)
        fun strings() = listOf<Updates>()
        fun containers() = listOf(addToSet, pop, pull, pullAll, push)
        fun common() = values().toList() - numerics() - strings() - containers()
    }

    open fun handle(target: JavaClassSource, property: CritterProperty) {
        handle(target, property, name, UpdateSieve.functions, "UpdateOperators")
    }

    open fun handle(target: Builder, field: PropertySpec) {
        handle(target, field, name, UpdateSieve.functions, "UpdateOperators")
    }
}
