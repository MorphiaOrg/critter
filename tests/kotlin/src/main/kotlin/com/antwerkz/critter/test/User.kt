package com.antwerkz.critter.test

@org.mongodb.morphia.annotations.Entity
class User : Person() {
    var email: String? = null
}
