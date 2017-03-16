package com.antwerkz.critter.test.kotlin

import org.mongodb.morphia.annotations.Entity

@Entity
class User : Person() {
    var email: String? = null
}
