package com.antwerkz.critter.test.kotlin

import org.mongodb.morphia.annotations.Entity

@Entity
class KotlinUser : KotlinPerson() {
    var email: String? = null
}
