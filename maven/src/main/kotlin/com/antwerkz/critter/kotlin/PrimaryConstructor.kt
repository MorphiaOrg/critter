package com.antwerkz.critter.kotlin

import com.antwerkz.critter.CritterConstructor
import com.antwerkz.critter.CritterFunction
import com.antwerkz.critter.Visible
import com.antwerkz.kibble.model.Constructor
import com.antwerkz.kibble.model.Visibility.INTERNAL
import com.antwerkz.kibble.model.Visibility.PRIVATE
import com.antwerkz.kibble.model.Visibility.PROTECTED
import com.antwerkz.kibble.model.Visibility.PUBLIC

open class PrimaryConstructor(val constructor: Constructor) : CritterConstructor {
    override fun setBody(body: String): CritterFunction {
        constructor.body = body
        return this
    }

    override fun isPublic() = constructor.isPublic()
    override fun setPublic(): CritterConstructor {
        constructor.visibility = PUBLIC
        return this
    }

    override fun isPrivate() = constructor.isPrivate()
    override fun setPrivate(): CritterConstructor {
        constructor.visibility = PRIVATE
        return this
    }

    override fun isProtected() = constructor.isProtected()
    override fun setProtected(): CritterConstructor {
        constructor.visibility = PROTECTED
        return this
    }

    override fun isInternal() = constructor.isInternal()
    override fun setInternal(): CritterConstructor {
        constructor.visibility = INTERNAL
        return this
    }

    override fun isPackagePrivate() = false
    override fun setPackagePrivate() = throw Visible.invalid("package private", "kotlin")

    override fun addParameter(type: String, name: String): CritterConstructor {
        constructor.addParameter(name, type)
        return this
    }

    override fun addParameter(type: Class<*>, name: String): CritterConstructor {
        constructor.addParameter(name, type.javaClass.name)
        return this
    }
}