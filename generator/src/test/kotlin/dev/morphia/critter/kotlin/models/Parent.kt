package dev.morphia.critter.kotlin.models

open class Parent(val name: String)

class Child(val age: Int, name: String, val nickNames: List<String>) : Parent(name)