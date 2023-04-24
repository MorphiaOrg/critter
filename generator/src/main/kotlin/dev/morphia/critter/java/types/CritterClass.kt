package dev.morphia.critter.java.types

import dev.morphia.critter.java.CritterType
import dev.morphia.critter.java.JavaContext
import dev.morphia.critter.java.extensions.ignored
import java.util.TreeMap
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource
import org.jboss.forge.roaster.model.source.PropertySource

class CritterClass(context: JavaContext, val source: JavaClassSource) : CritterType(context, source) {
    override fun superType(): CritterClass? {
        return context.classes[source.superType] as CritterClass?
    }

    override fun superTypes(): List<CritterType> {
        val list = mutableListOf<CritterType>()
        superType()?.let { type ->
            list += type
            list += interfaces()

            list += type.superTypes()
        }

        return list
    }

    override fun isAbstract(): Boolean {
        return source.isAbstract
    }

    override fun allProperties(): List<PropertySource<JavaClassSource>> {
        return (superType()?.allProperties() ?: listOf()) +
            source.properties
                .filter { !it.ignored() }
    }


    override fun constructors() = source.methods
        .filter { it.isConstructor }


    override fun bestConstructor(): MethodSource<JavaClassSource>? {
        val propertyMap = allProperties()
            .map { it.name to it.type }
            .toMap(TreeMap())
        val matches = constructors()
            .filter {
                it.parameters.all { param ->
                    propertyMap[param.name].toString() == param.type.toString()
                }
            }
            .sortedBy { it.parameters.size }
            .reversed()

        return matches.firstOrNull()
    }

}