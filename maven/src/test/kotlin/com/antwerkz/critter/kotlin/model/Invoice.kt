/**
 * Copyright (C) 2012-2013 Justin Lee <jlee></jlee>@antwerkz.com>

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter.kotlin.model

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Reference
import java.time.LocalDateTime
import java.util.ArrayList

@Entity
class Invoice {
    @Id
    private val id: ObjectId = ObjectId()

    var date: LocalDateTime? = null

    @Reference
    var person: Person? = null

    @Embedded
    var addresses: MutableList<Address>? = null

    var total: Double = 0.0

    private var items: MutableList<Item>? = null

    constructor() {}

    constructor(date: LocalDateTime, person: Person, address: Address, vararg items: Item) {
        this.date = date
        this.person = person
        add(address)
        for (item in items) {
            add(item)
        }
    }

    fun add(item: Item) {
        if (items == null) {
            items = ArrayList<Item>()
        }
        items!!.add(item)
        total += item.price
    }

    fun add(address: Address) {
        if (addresses == null) {
            addresses = ArrayList<Address>()
        }
        addresses!!.add(address)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val invoice = o as Invoice?
        if (id != invoice!!.id) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Invoice{" +
                "id=" + id +
                ", date=" + date +
                ", person=" + person +
                ", addresses=" + addresses +
                ", total=" + total +
                ", items=" + items +
                '}'
    }
}
