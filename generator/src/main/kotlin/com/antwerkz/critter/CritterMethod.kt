package com.antwerkz.critter

interface CritterFunction {
    fun setBody(body: String): CritterFunction
    fun addParameter(type: String, name: String): CritterFunction
    fun addParameter(type: Class<*>, name: String): CritterFunction
}

interface CritterConstructor: CritterFunction, Visible

interface CritterMethod: CritterFunction, Visible {
    fun setName(name: String): CritterMethod
    fun getReturnType(): String
    fun setReturnType(type: String): CritterMethod
    fun setReturnType(klass: Class<*>): CritterMethod
}

