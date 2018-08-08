package com.antwerkz.critter

import com.antwerkz.critter.Visibility.INTERNAL
import com.antwerkz.critter.Visibility.PACKAGE
import com.antwerkz.critter.Visibility.PRIVATE
import com.antwerkz.critter.Visibility.PROTECTED
import com.antwerkz.critter.Visibility.PUBLIC

interface Visible {
    companion object {
        fun invalid(name: String, type: String): IllegalStateException {
            return IllegalStateException("'$name' is not supported for $type")
        }
    }

    var visibility: Visibility

    fun isInternal() = visibility == INTERNAL
    fun isPublic() = visibility == PUBLIC
    fun isPackagePrivate() = visibility == PACKAGE
    fun isPrivate() = visibility == PRIVATE

    fun isProtected() = visibility == PROTECTED

    fun test() {
        Visible.invalid("asdflkj", "alsdkjf")
    }
}

enum class Visibility {
    PRIVATE,
    PACKAGE,
    INTERNAL,
    PROTECTED,
    PUBLIC
}
