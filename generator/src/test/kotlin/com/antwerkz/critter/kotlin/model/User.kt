package com.antwerkz.critter.kotlin.model

import org.mongodb.morphia.annotations.Entity

@Entity
class User : Person() {
    var email: String? = null
}
