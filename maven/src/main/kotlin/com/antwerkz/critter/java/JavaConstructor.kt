package com.antwerkz.critter.java

import com.antwerkz.critter.CritterConstructor
import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.MethodSource

class JavaConstructor(private val method: MethodSource<JavaClassSource>): CritterConstructor {
    init {
        method.isConstructor = true
    }
    override fun addParameter(type: String, name: String): CritterConstructor {
        method.addParameter(type, name)
        return this
    }

    override fun addParameter(type: Class<*>, name: String): CritterConstructor {
        method.addParameter(type, name)
        return this
    }

    override fun isPublic() = method.isPublic
    override fun setPublic(): CritterConstructor {
        method.setPublic()
        return this
    }

    override fun isInternal() = false
    override fun setInternal(): CritterConstructor {
        throw IllegalStateException("'internal' is not supported on Java type")
    }

    override fun isPackagePrivate() = method.isPackagePrivate
    override fun setPackagePrivate(): CritterConstructor {
        method.setPackagePrivate()
        return this
    }

    override fun isPrivate() = method.isPrivate
    override fun setPrivate(): CritterConstructor {
        method.setPrivate()
        return this
    }

    override fun isProtected() = method.isProtected
    override fun setProtected(): CritterConstructor {
        method.setProtected()
        return this
    }

    override fun setBody(body: String): CritterConstructor {
        method.body = body
        return this
    }
}