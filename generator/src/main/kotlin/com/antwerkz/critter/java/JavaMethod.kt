package com.antwerkz.critter.java

import com.antwerkz.critter.CritterMethod
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource

open class JavaMethod(internal val method: MethodSource<JavaClassSource>) : CritterMethod {
    override fun addParameter(type: String, name: String): CritterMethod {
        method.addParameter(type, name)
        return this
    }

    override fun addParameter(type: Class<*>, name: String): CritterMethod {
        method.addParameter(type, name)
        return this
    }

    override fun isPublic() = method.isPublic
    override fun setPublic(): CritterMethod {
        method.setPublic()
        return this
    }

    override fun isInternal() = false
    override fun setInternal(): CritterMethod {
        throw IllegalStateException("'internal' is not supported on Java type")
    }

    override fun isPackagePrivate() = method.isPackagePrivate
    override fun setPackagePrivate(): CritterMethod {
        method.setPackagePrivate()
        return this
    }

    override fun isPrivate() = method.isPrivate
    override fun setPrivate(): CritterMethod {
        method.setPrivate()
        return this
    }

    override fun isProtected() = method.isProtected
    override fun setProtected(): CritterMethod {
        method.setProtected()
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