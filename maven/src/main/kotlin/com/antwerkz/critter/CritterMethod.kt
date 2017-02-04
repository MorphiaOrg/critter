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

class JavaMethod(private val method: MethodSource<JavaClassSource>) : CritterMethod {
    override fun setConstructor(constructor: Boolean): CritterMethod {
        method.isConstructor = constructor
        return this
    }

    override fun addParameter(type: String, name: String): CritterMethod {
        method.addParameter(type, name)
        return this
    }

    override fun addParameter(type: Class<*>, name: String): CritterMethod {
        method.addParameter(type, name)
        return this
    }

    override fun setPublic(): CritterMethod {
        method.setPublic()
        return this
    }

    override fun setName(name: String): CritterMethod {
        method.name = name
        return this
    }

    override fun getReturnType(): String {
        return method.returnType.name
    }

    override fun setReturnType(klass: Class<*>): CritterMethod {
        method.setReturnType(klass)
        return this
    }

    override fun setReturnType(type: String): CritterMethod {
        method.setReturnType(type)
        return this
    }

    override fun setBody(body: String): CritterMethod {
        method.body = body
        return this
    }
}