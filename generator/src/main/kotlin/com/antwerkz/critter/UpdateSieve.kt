package com.antwerkz.critter

import com.antwerkz.critter.Critter.addMethods
import com.antwerkz.critter.kotlin.isContainer
import com.antwerkz.critter.kotlin.isNumeric
import com.antwerkz.critter.kotlin.isText
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.query.experimental.updates.AddToSetOperator
import dev.morphia.query.experimental.updates.UpdateOperator
import dev.morphia.query.experimental.updates.UpdateOperators
import org.jboss.forge.roaster.model.source.JavaClassSource
import java.util.TreeSet
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
object UpdateSieve {
    internal val functions = updates()
            .map { it.name to it }
            .toMap()
            .toSortedMap()

    @ExperimentalStdlibApi
    fun updates() = UpdateOperators::class.functions
            .filter { typeOf<UpdateOperator>().isSupertypeOf(it.returnType) }
            .filter { it.name !in setOf("setOnInsert") }
            .filterNot { it.name == "set" && it.parameters.size == 1 }

    fun handlers(field: CritterField, target: JavaClassSource) {
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
@ExperimentalStdlibApi
enum class Updates : OperationGenerator {
    and,
    addToSet {
        override fun handle(target: JavaClassSource, field: CritterField) {
            target.addImport(AddToSetOperator::class.java)
            target.addMethods("""
            public AddToSetOperator ${name}(Object value) {
                return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value);
            }         
            
            public AddToSetOperator ${name}(List<?> values) {
                return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), values);
            } """.trimIndent())
        }

        override fun handle(target: Builder, field: PropertySpec) {
            target.addFunction(FunSpec.builder(name)
                    .addParameter("value", typeOf<Any>().asTypeName())
                    .returns(AddToSetOperator::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value)""")
                    .build())

            target.addFunction(FunSpec.builder(name)
                    .addParameter("values", typeOf<List<Any>>().asTypeName())
                    .returns(AddToSetOperator::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), values)""")
                    .build())
        }
    },
    currentDate,
    dec {
        override fun handle(target: JavaClassSource, field: CritterField) {
            target.addMethod("""
                public UpdateOperator ${name}() {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"));
                } """.trimIndent())
            target.addMethod("""
                public UpdateOperator ${name}(Number value) {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value);
                } """.trimIndent())
        }

        override fun handle(target: Builder, field: PropertySpec) {
            target.addFunction(FunSpec.builder(name)
                    .addCode("""return UpdateOperators.${name}(extendPath(prefix, "${field.name}"))""")
                    .build())
            target.addFunction(FunSpec.builder(name)
                    .addParameter("value", Number::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value)""")
                    .build())
        }
    },
    inc {
        override fun handle(target: JavaClassSource, field: CritterField) {
            target.addMethod("""
                public UpdateOperator ${name}() {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"));
                } """.trimIndent())
            target.addMethod("""
                public UpdateOperator ${name}(Number value) {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value);
                } """.trimIndent())
        }

        override fun handle(target: Builder, field: PropertySpec) {
            target.addFunction(FunSpec.builder(name)
                    .addCode("""return UpdateOperators.${name}(extendPath(prefix, "${field.name}"))""")
                    .build())
            target.addFunction(FunSpec.builder(name)
                    .addParameter("value", Number::class.asClassName())
                    .addCode("""return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value)""")
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
        override fun handle(target: JavaClassSource, field: CritterField) {
            target.addImport(java.util.List::class.java)
            target.addMethod("""
            public UpdateOperator ${name}(List<?> values) {
                return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), values);
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

    open fun handle(target: JavaClassSource, field: CritterField) {
        handle(target, field, name, UpdateSieve.functions, "UpdateOperators")
    }

    open fun handle(target: Builder, field: PropertySpec) {
        handle(target, field, name, UpdateSieve.functions, "UpdateOperators")
    }
}
