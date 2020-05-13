package com.antwerkz.critter

import com.antwerkz.critter.Critter.addMethods
import com.antwerkz.critter.kotlin.isContainer
import com.antwerkz.critter.kotlin.isNumeric
import com.antwerkz.critter.kotlin.isText
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
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
    internal val functions = filters()
            .map { it.name to it }
            .toMap()
            .toSortedMap()


    @ExperimentalStdlibApi
    fun filters() = UpdateOperators::class.functions
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
            it.handle(field, target)
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
            it.handle(field, target)
        }
    }
}

@Suppress("EnumEntryName")
@ExperimentalStdlibApi
enum class Updates {
    addToSet {
        override fun handle(field: CritterField, target: JavaClassSource) {
            target.addImport(AddToSetOperator::class.java)
            target.addMethods("""
            public AddToSetOperator ${name}(Object value) {
                return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value);
            }         
            
            public AddToSetOperator ${name}(List<?> values) {
                return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), values);
            } """.trimIndent())
        }

        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
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
    dec {
        override fun handle(field: CritterField, target: JavaClassSource) {
            target.addMethod("""
                public UpdateOperator ${name}() {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"));
                } """.trimIndent())
            target.addMethod("""
                public UpdateOperator ${name}(Number value) {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value);
                } """.trimIndent())
        }

        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
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
        override fun handle(field: CritterField, target: JavaClassSource) {
            target.addMethod("""
                public UpdateOperator ${name}() {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"));
                } """.trimIndent())
            target.addMethod("""
                public UpdateOperator ${name}(Number value) {
                    return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), value);
                } """.trimIndent())
        }

        override fun handle(field: PropertySpec, target: TypeSpec.Builder) {
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
    pop,
    pull,
    pullAll {
        override fun handle(field: CritterField, target: JavaClassSource) {
            target.addImport(java.util.List::class.java)
            target.addMethod("""
            public UpdateOperator ${name}(List<?> values) {
                return UpdateOperators.${name}(extendPath(prefix, "${field.name}"), values);
            } """.trimIndent())
        }
    },
    push,
    set,
    unset;

    companion object {
        fun get(name: String) = valueOf(name)

        fun numerics(): List<Updates> = listOf(dec, inc, max, min)

        fun strings() = listOf<Updates>()

        fun containers() = listOf(addToSet, pop, pull, pullAll, push)
        
        fun common() = values().toList() - numerics() - strings() - containers()
    }
    
    open fun handle(field: CritterField, target: JavaClassSource) {
        target.addImport(UpdateOperators::class.java.name)
        target.addImport(UpdateOperator::class.java.name)

        val kFunction = UpdateSieve.functions[name] ?:  TODO("no handler for $name")

        if (kFunction.parameters[0].type.javaType != String::class.java) {
            throw UnsupportedOperationException("Parameters are nonstandard.  '${kFunction}' needs a custom implementation")
        }
        val params = kFunction.parameters.drop(1).map {
            "${it.type.javaType.asTypeName()} ${it.name}"
        }.joinToString(", ")
        val args = kFunction.parameters.drop(1).map { it.name }.joinToString(", ")
        val returnType = kFunction.returnType.asTypeName()
        var parameters = """extendPath(prefix, "${field.name}")"""
        if(args.isNotBlank()) {
            parameters += ", ${args}"
        }
        target.addMethod("""
            public ${returnType} ${kFunction.name}(${params}) {
                return UpdateOperators.${kFunction.name}(${parameters});
            } """.trimIndent())
    }

    open fun handle(field: PropertySpec, target: TypeSpec.Builder) {
        val kFunction = UpdateSieve.functions[name] ?: TODO("no handler for $name")

        if (kFunction.parameters[0].type.asTypeName() != String::class.asClassName()) {
            throw UnsupportedOperationException("Parameters are nonstandard.  '${kFunction}' needs a custom implementation")
        }
        val params = kFunction.parameters.drop(1).map {
            ParameterSpec.builder(it.name!!, it.type.asTypeName()).build()
        }
        val args = kFunction.parameters.drop(1).map { it.name }.joinToString(", ")
        var parameters = """extendPath(prefix, "${field.name}")"""
        if(args.isNotBlank()) {
            parameters += ", ${args}"
        }
        target.addFunction(FunSpec
                .builder(kFunction.name)
                .addParameters(params)
                .addCode("""return UpdateOperators.${kFunction.name}(${parameters})""")
                .build())
    }

}


