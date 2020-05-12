/*
 * Copyright (C) 2012-2017 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter.test

import com.antwerkz.critter.test.criteria.InvoiceCriteria
import com.antwerkz.critter.test.criteria.InvoiceCriteria.Companion.addresses
import com.antwerkz.critter.test.criteria.PersonCriteria.Companion.age
import com.antwerkz.critter.test.criteria.PersonCriteria.Companion.first
import com.antwerkz.critter.test.criteria.PersonCriteria.Companion.last
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern.MAJORITY
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import dev.morphia.DeleteOptions
import dev.morphia.Morphia
import dev.morphia.UpdateOptions
import dev.morphia.query.FindOptions
import dev.morphia.query.Sort
import dev.morphia.query.experimental.filters.Filters
import dev.morphia.query.experimental.filters.Filters.and
import org.bson.UuidRepresentation.STANDARD
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test
import java.time.LocalDateTime

@Test
class KotlinCriteriaTest {

    @AfterMethod
    fun clean() {2
        val mongo = com.mongodb.MongoClient()
        val critter = mongo.getDatabase("critter")
        critter.drop()
    }

    val mongo: MongoClient by lazy {
        val builder = MongoClientSettings.builder()

        try {
            builder.uuidRepresentation(STANDARD)
        } catch (ignored: java.lang.Exception) {
            // not a 4.0 driver
        }

        MongoClients.create(builder
                .build())
    }

    val datastore: dev.morphia.Datastore by lazy {
        val critter = mongo.getDatabase("critter")
        val ds = Morphia.createDatastore(mongo, "critter")
        ds.mapper.mapPackage("com.antwerkz")
        critter.drop()

        ds
    }

    fun testInvoice() {
        val john = Person("John", "Doe")
        datastore.save(john)
        datastore.save(Invoice(LocalDateTime.of(2012, 12, 21, 13, 15), john, Address("New York City", "NY", "10000"),
                Item("ball", 5.0), Item("skateboard", 17.35)))
        val jeff = Person("Jeff", "Johnson")
        datastore.save(jeff)
        datastore.save(Invoice(LocalDateTime.of(2006, 3, 4, 8, 7), jeff, Address("Los Angeles", "CA", "90210"),
                Item("movie", 29.95)))
        val sally = Person("Sally", "Ride")
        datastore.save(sally)
        datastore.save(Invoice(LocalDateTime.of(2007, 8, 16, 19, 27), sally, Address("Chicago", "IL", "99999"),
                Item("kleenex", 3.49), Item("cough and cold syrup", 5.61)))
        val invoice = datastore.find(Invoice::class.java)
                .filter(InvoiceCriteria.person().eq(john))
                .first()
        Assert.assertEquals(invoice.person?.last, "Doe")

        val byCity = datastore.find(Invoice::class.java)
                .filter(addresses().city().eq("Chicago"))
                .first()
        Assert.assertNotNull(byCity)

        val critter = datastore.find(Invoice::class.java)
                .filter(addresses().city().eq("Chicago"))
                .first()
        Assert.assertNotNull(critter)
        Assert.assertEquals(critter, byCity)
    }

    @Test
    fun updates() {
        val query = datastore.find(Person::class.java)
        query.delete()

        query.filter(first().eq("Jim"), last().eq("Beam"))

        Assert.assertEquals(query.update(age().set(30L))
                .execute(UpdateOptions().multi(true)).modifiedCount, 0)

        Assert.assertNotNull(query.update(age().set(30L))
                .execute(UpdateOptions().upsert(true)).upsertedId)

        val update = query.update(age().inc())
                .execute(UpdateOptions().multi(true))
        Assert.assertEquals(update.modifiedCount, 1)
        val get: Person = datastore.find(Person::class.java).first() as Person
        Assert.assertEquals(get.age, 31L)

        Assert.assertNotNull(datastore.find(Person::class.java).first())

        val delete = query.delete()
        Assert.assertEquals(delete.deletedCount, 1)
    }

    @Test
    fun removes() {
        for (i in 0..99) {
            datastore.save(Person("First$i", "Last$i"))
        }
        var query = datastore.find(Person::class.java)
                .filter(last().regex().pattern("Last2"))
        var result = query.delete(DeleteOptions().multi(true))
        Assert.assertEquals(result.deletedCount, 11)
        Assert.assertEquals(query.count(), 0)

        query = datastore.find(Person::class.java)
        Assert.assertEquals(query.count(), 89)

        query = datastore.find(Person::class.java)
                .filter(last().regex().pattern("Last3"))
        result = query.delete(DeleteOptions()
                .multi(true)
                .writeConcern(MAJORITY))
        Assert.assertEquals(result.deletedCount, 11)
        Assert.assertEquals(query.count(), 0)
    }

    fun embeds() {
        var invoice = Invoice()
        invoice.orderDate = LocalDateTime.now()
        var person = Person("Mike", "Bloomberg")
        datastore.save(person)
        invoice.person = person
        invoice.add(Address("New York City", "NY", "10036"))
        datastore.save(invoice)

        invoice = Invoice()
        invoice.orderDate = LocalDateTime.now()
        person = Person("Andy", "Warhol")
        datastore.save(person)

        invoice.person = person
        invoice.add(Address("NYC", "NY", "10018"))
        datastore.save(invoice)

        val query = datastore.find(Invoice::class.java)
        Assert.assertEquals(query.iterator(FindOptions()
                .sort(Sort.ascending(addresses().city().path()))).next().addresses!![0].city, "NYC")

        Assert.assertEquals(query.first(FindOptions()
                .sort(Sort.descending(addresses().city().path())))?.addresses!![0].city, "New York City")
    }

    fun orQueries() {
        datastore.save(Person("Mike", "Bloomberg"))
        datastore.save(Person("Mike", "Tyson"))
        val query1 = datastore.find<Person>(Person::class.java)
                .filter(Filters.or(
                                Filters.eq("last", "Bloomberg"),
                                Filters.eq("last", "Tyson")
                        ))

        val query2 = datastore.find(Person::class.java)
                .filter(Filters.or(
                        last().eq("Bloomberg"),
                        last().eq("Tyson")))

        Assert.assertEquals(query2.toList().size, 2)
        Assert.assertEquals(query1.toList(), query2.toList())
    }

    fun andQueries() {
        datastore.save(Person("Mike", "Bloomberg"))
        datastore.save(Person("Mike", "Tyson"))
        val query1 = datastore.find(Person::class.java)
                .filter(and(
                        Filters.eq("first", "Mike"),
                        Filters.eq("last", "Tyson")))

        val query2 = datastore.find(Person::class.java)
                .filter(Filters.and(
                        first().eq("Mike"),
                        last().eq("Tyson")))

        Assert.assertEquals(query2.toList().size, 1)
        Assert.assertEquals(query1.toList(), query2.toList())
    }
}
