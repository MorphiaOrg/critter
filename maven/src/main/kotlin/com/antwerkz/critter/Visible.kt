package com.antwerkz.critter

interface Visible<out T> {
    companion object {
        fun invalid(name: String, type: String): IllegalStateException {
            return IllegalStateException("'$name' is not supported for $type")
        }
    }

    fun isInternal(): Boolean
    fun setInternal(): T
    fun isPublic(): Boolean
    fun setPublic(): T
    fun isPackagePrivate(): Boolean
    fun setPackagePrivate(): T
    fun isPrivate(): Boolean
    fun setPrivate(): T
    fun isProtected(): Boolean
    fun setProtected(): T
}