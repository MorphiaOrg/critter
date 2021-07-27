package dev.morphia.critter

import dev.morphia.critter.CritterType.Companion.DOCUMENT
import dev.morphia.critter.CritterType.Companion.MAPPER
import org.jboss.forge.roaster.model.source.MethodSource

class CritterMethod(val name: String, val parameters: List<CritterType>, val returnType: CritterType) {
    val annotations = mutableListOf<CritterAnnotation>()

    override fun toString(): String {
        return "CritterMethod(name='$name', parameters=$parameters, returnType=$returnType, annotations=$annotations)"
    }
    fun parameterNames(): List<String> {
        return parameters.map {
            when (it) {
                DOCUMENT -> "document"
                MAPPER -> "mapper"
                else -> "instance"
            }
        }
    }
}

fun MethodSource<*>.toCritter(): CritterMethod {
    return CritterMethod(name, parameters.map { it.type.toCritter() }, returnType.toCritter())
}
