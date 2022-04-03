package dev.morphia.critter

import org.jboss.forge.roaster.model.source.ParameterSource

data class CritterParameter(val name: String, val type: CritterType, val isArray: Boolean, val annotations: List<CritterAnnotation>) {
}

fun ParameterSource<*>.toCritter(): CritterParameter {
    return CritterParameter(name, type.toCritter(), isVarArgs, this.annotations.map { it.toCritter() })
}