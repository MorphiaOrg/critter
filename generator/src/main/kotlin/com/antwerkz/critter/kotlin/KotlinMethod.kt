package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterMethod
import com.antwerkz.critter.Visible.Companion.invalid
import com.antwerkz.kibble.model.KibbleFunction
import com.antwerkz.kibble.model.Visibility.INTERNAL
import com.antwerkz.kibble.model.Visibility.PRIVATE
import com.antwerkz.kibble.model.Visibility.PROTECTED
import com.antwerkz.kibble.model.Visibility.PUBLIC

class KotlinMethod(val source: KibbleFunction) : CritterMethod {
    override fun isPublic() = source.isPublic()
    override fun setPublic(): CritterMethod {
        source.visibility = PUBLIC
        return this
    }

    override fun isPrivate() = source.isPrivate()
    override fun setPrivate(): CritterMethod {
        source.visibility = PRIVATE
        return this
    }

    override fun isProtected() = source.isProtected()
    override fun setProtected(): CritterMethod {
        source.visibility = PROTECTED
        return this
    }

    override fun isInternal() = source.isInternal()
    override fun setInternal(): CritterMethod {
        source.visibility = INTERNAL
        return this
    }

    override fun isPackagePrivate() = false
    override fun setPackagePrivate() = throw invalid("package private", "kotlin")

    override fun setName(name: String): CritterMethod {
        source.name = name
        return this
    }

    override fun getReturnType(): String {
        return source.type
    }

    override fun setReturnType(type: String): CritterMethod {
        source.type = type
        return this
    }

    override fun setReturnType(klass: Class<*>): CritterMethod {
        source.type = klass.name
        return this
    }

    override fun setBody(body: String): CritterMethod {
        source.body = body
        return this
    }

    override fun addParameter(type: String, name: String): CritterMethod {
        source.addParameter(name, type)
        return this
    }

    override fun addParameter(type: Class<*>, name: String): CritterMethod {
        source.addParameter(name, type.name)
        return this
    }
}