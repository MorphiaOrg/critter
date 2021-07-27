package dev.morphia.critter

import org.jboss.forge.roaster.model.source.MethodSource

class CritterMethod(val name: String, val parameters: List<CritterType>, val returnType: CritterType) {
    val annotations = mutableListOf<CritterAnnotation>()

    override fun toString(): String {
        return "CritterMethod(name='$name', parameters=$parameters, returnType=$returnType, annotations=$annotations)"
    }
}

fun MethodSource<*>.toCritter(): CritterMethod {
    return CritterMethod(name, parameters.map { it.type.toCritter() }, returnType.toCritter())
}
