package com.antwerkz.critter

import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource

interface CritterMethod {
    fun setPublic(): CritterMethod
    fun setName(name: String): CritterMethod
    fun getReturnType(): String
    fun setReturnType(type: String): CritterMethod
    fun setReturnType(klass: Class<*>): CritterMethod
    fun setBody(body: String): CritterMethod
    fun addParameter(type: String, name: String): CritterMethod
    fun addParameter(type: Class<*>, name: String): CritterMethod
    fun setConstructor(constructor: Boolean): CritterMethod

}

