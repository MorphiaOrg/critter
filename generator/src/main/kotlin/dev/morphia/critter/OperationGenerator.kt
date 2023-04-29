package dev.morphia.critter

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.morphia.query.filters.Filter
import dev.morphia.query.filters.Filters
import dev.morphia.query.updates.UpdateOperator
import dev.morphia.query.updates.UpdateOperators
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.PropertySource

interface OperationGenerator {
    fun handle(target: JavaClassSource, property: PropertySource<*>, name: String, functions: Map<String, KFunction<*>>, functionSource: String) {
        target.addImport(Filters::class.java)
        target.addImport(Filter::class.java)
        target.addImport(UpdateOperators::class.java.name)
        target.addImport(UpdateOperator::class.java.name)

        val kFunction = functions[name] ?: TODO("no handler for $name")

        if (kFunction.parameters[0].type.javaType != String::class.java) {
            throw UnsupportedOperationException("Parameters are nonstandard.  '${kFunction}' needs a custom implementation")
        }
        val params = kFunction.parameters.drop(1).map {
            "${it.type.javaType.typeName} ${it.name}"
        }.joinToString(", ")
        val args = kFunction.parameters.drop(1).map { it.name }.joinToString(", ")
        val returnType = kFunction.returnType.javaType.typeName
        var parameters = """path"""
        if (args.isNotBlank()) {
            parameters += ", ${args}"
        }
        target.addMethod("""
            public ${returnType} ${kFunction.name}(${params}) {
                return ${functionSource}.${kFunction.name}(${parameters});
            } """.trimIndent())
    }

    fun handle(target: Builder, property: KSPropertyDeclaration, name: String, functions: Map<String, KFunction<*>>,
               functionSource: String) {
        val kFunction = functions[name] ?: TODO("no handler for $name")

        if (kFunction.parameters[0].type.asTypeName() != String::class.asClassName()) {
            throw UnsupportedOperationException("Parameters are nonstandard.  '${kFunction}' needs a custom implementation")
        }
        val params = kFunction.parameters.drop(1).map {
            ParameterSpec.builder("__${it.name}", it.type.asTypeName()).build()
        }
        val parameters = (mutableListOf("path") + params.map { it.name })
            .joinToString(", ")
        target.addFunction(FunSpec
                .builder(kFunction.name)
                .addParameters(params)
                .addCode("""return $functionSource.${kFunction.name}(${parameters})""")
                .build())
    }
}