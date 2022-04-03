package dev.morphia.critter

import dev.morphia.critter.CritterType.Companion.DOCUMENT
import dev.morphia.critter.CritterType.Companion.MAPPER
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.ParameterSource

class CritterMethod(val name: String, val parameters: List<CritterParameter>, val returnType: CritterType?) {
    val annotations = mutableListOf<CritterAnnotation>()

    override fun toString(): String {
        return "CritterMethod(name='$name', parameters=$parameters, returnType=$returnType, annotations=$annotations)"
    }

    fun parameterNames(): List<String> {
        return parameters.map {
            when (it.type) {
                DOCUMENT -> "document"
                MAPPER -> "mapper"
                else -> "instance"
            }
        }
    }
}

fun MethodSource<*>.toCritter(): CritterMethod {
    return CritterMethod(name, parameters
        .map { param: ParameterSource<*> -> param.toCritter() }, returnType?.toCritter())
}
