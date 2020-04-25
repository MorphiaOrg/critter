package com.antwerkz.critter

import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource

object Critter {
    fun JavaClassSource.addMethods(methods: String): List<MethodSource<JavaClassSource>> {
        val parse = Roaster.parse("""
            class ${name} {
                $methods            
            }
            """.trimIndent()) as JavaClassSource
        val list = parse.methods
        return list.map {
            addMethod(it)
        }
    }
}