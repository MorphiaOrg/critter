package com.antwerkz.critter.test

import dev.morphia.annotations.Entity

@Entity
class User : AbstractPerson() {
    var email: String? = null
}
