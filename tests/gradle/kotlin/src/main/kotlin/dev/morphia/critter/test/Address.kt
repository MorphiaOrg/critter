package dev.morphia.critter.test

import dev.morphia.annotations.Entity
import dev.morphia.annotations.Property

@Entity
class Address {
    @Property("c")
    var city: String? = null
    var state: String? = null
    var zip: String? = null

    constructor() {}

    constructor(city: String, state: String, zip: String) {
        this.city = city
        this.state = state
        this.zip = zip
    }

    override fun toString(): String {
        return "Address{" +
                "city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip='" + zip + '\'' +
                '}'
    }
}
