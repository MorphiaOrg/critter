package com.antwerkz.critter.test

import org.mongodb.morphia.annotations.Entity

@Entity
class User : AbstractPerson() {
    var email: String? = null
}
