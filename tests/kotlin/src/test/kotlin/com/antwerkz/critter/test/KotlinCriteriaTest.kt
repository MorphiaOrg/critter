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
import com.antwerkz.critter.test.criteria.PersonCriteria
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

@Test
class KotlinCriteriaTest {

    @AfterMethod
    fun clean() {
        val mongo = com.mongodb.MongoClient()
        val critter = mongo.getDB("critter")
        critter.dropDatabase()
    }

    val datastore: org.mongodb.morphia.Datastore by lazy {
        val mongo = com.mongodb.MongoClient()
        val critter = mongo.getDB("critter")
        critter.dropDatabase()
        val morphia = org.mongodb.morphia.Morphia()
        morphia.mapPackage("com.antwerkz")
        morphia.createDatastore(mongo, "critter")
    }

    fun invoice() {
        val john = com.antwerkz.critter.test.Person("John", "Doe")
        datastore.save(john)
        datastore.save(Invoice(java.time.LocalDateTime.of(2012, 12, 21, 13, 15), john, Address("New York City", "NY", "10000"),
                Item("ball", 5.0), Item("skateboard", 17.35)))
        val jeff = com.antwerkz.critter.test.Person("Jeff", "Johnson")
        datastore.save(jeff)
        datastore.save(Invoice(java.time.LocalDateTime.of(2006, 3, 4, 8, 7), jeff, Address("Los Angeles", "CA", "90210"),
                Item("movie", 29.95)))
        val sally = com.antwerkz.critter.test.Person("Sally", "Ride")
        datastore.save(sally)
        datastore.save(Invoice(java.time.LocalDateTime.of(2007, 8, 16, 19, 27), sally, Address("Chicago", "IL", "99999"),
                Item("kleenex", 3.49), Item("cough and cold syrup", 5.61)))
        var invoiceCriteria = InvoiceCriteria(datastore)
        invoiceCriteria.person(john)
        val invoice = invoiceCriteria.query().get()
        val doe = datastore.createQuery<Invoice>(Invoice::class.java).filter("person =", john).get()
        org.testng.Assert.assertEquals(invoice, doe)
        org.testng.Assert.assertEquals(doe.person?.last, "Doe")
        org.testng.Assert.assertEquals(invoice.person?.last, "Doe")
        val query = datastore.createQuery<Invoice>(Invoice::class.java).field("addresses.city").equal("Chicago").get()
        org.testng.Assert.assertNotNull(query)
        invoiceCriteria = InvoiceCriteria(datastore)
        invoiceCriteria.addresses().city("Chicago")
        val critter = invoiceCriteria.query().get()
        org.testng.Assert.assertNotNull(critter)
        org.testng.Assert.assertEquals(critter, query)
    }

    @Test
    fun updates() {
        val personCriteria = PersonCriteria(datastore)
        personCriteria.delete()
        personCriteria.first("Jim")
        personCriteria.last("Beam")

        val query = personCriteria.query()

        org.testng.Assert.assertEquals(personCriteria.updater()
                .age(30L)
                .updateAll().updatedCount, 0)

        org.testng.Assert.assertEquals(personCriteria.updater()
                .age(30L)
                .upsert().insertedCount, 1)

        val update = personCriteria.updater().incAge().updateAll()
        org.testng.Assert.assertEquals(update.updatedCount, 1)
        val get: Person = personCriteria.query().get() as Person
        org.testng.Assert.assertEquals(get.age, 31L)

        org.testng.Assert.assertNotNull(PersonCriteria(datastore).query().get())

        val delete = datastore.delete(query)
        org.testng.Assert.assertEquals(delete.n, 1)
    }

    @Test(enabled = false) // waiting morphia issue #711
    fun updateFirst() {
        for (i in 0..99) {
            datastore.save(com.antwerkz.critter.test.Person("First" + i, "Last" + i))
        }
        var criteria = PersonCriteria(datastore)
        criteria.last().contains("Last2")
        criteria.updater()
                .age(1000L)
                .updateFirst()

        criteria = PersonCriteria(datastore)
        criteria.age(1000L)

        //    Assert.assertEquals(criteria.query().countAll(), 1);
    }

    @Test
    fun removes() {
        for (i in 0..99) {
            datastore.save(com.antwerkz.critter.test.Person("First" + i, "Last" + i))
        }
        var criteria = PersonCriteria(datastore)
        criteria.last().contains("Last2")
        var result = criteria.updater()
                .remove()
        org.testng.Assert.assertEquals(result.n, 11)
        org.testng.Assert.assertEquals(criteria.query().count(), 0)

        criteria = PersonCriteria(datastore)
        org.testng.Assert.assertEquals(criteria.query().count(), 89)

        criteria = PersonCriteria(datastore)
        criteria.last().contains("Last3")
        result = criteria.updater().remove(com.mongodb.WriteConcern.MAJORITY)
        org.testng.Assert.assertEquals(result.n, 11)
        org.testng.Assert.assertEquals(criteria.query().count(), 0)
    }

    fun embeds() {
        var invoice = Invoice()
        invoice.date = java.time.LocalDateTime.now()
        var person = com.antwerkz.critter.test.Person("Mike", "Bloomberg")
        datastore.save(person)
        invoice.person = person
        invoice.add(com.antwerkz.critter.test.Address("New York City", "NY", "10036"))
        datastore.save(invoice)

        invoice = Invoice()
        invoice.date = java.time.LocalDateTime.now()
        person = com.antwerkz.critter.test.Person("Andy", "Warhol")
        datastore.save(person)

        invoice.person = person
        invoice.add(com.antwerkz.critter.test.Address("NYC", "NY", "10018"))
        datastore.save(invoice)

        val criteria1 = InvoiceCriteria(datastore)
        criteria1.addresses().city().order()
        val asList = criteria1.query().asList()
        org.testng.Assert.assertEquals(asList[0].addresses!![0].city, "NYC")

        val criteria2 = InvoiceCriteria(datastore)
        criteria2.addresses().city().order(false)
        org.testng.Assert.assertEquals(criteria2.query().asList()[0].addresses!![0].city, "New York City")
    }

    fun orQueries() {
        datastore.save(com.antwerkz.critter.test.Person("Mike", "Bloomberg"))
        datastore.save(com.antwerkz.critter.test.Person("Mike", "Tyson"))

        val query = datastore.createQuery<com.antwerkz.critter.test.Person>(com.antwerkz.critter.test.Person::class.java)
        query.or(
                query.criteria("last").equal("Bloomberg"),
                query.criteria("last").equal("Tyson")
        )

        val criteria = PersonCriteria(datastore)
        criteria.or(
                criteria.last("Bloomberg"),
                criteria.last("Tyson")
        )

        org.testng.Assert.assertEquals(criteria.query().asList().size, 2)
        org.testng.Assert.assertEquals(query.asList(), criteria.query().asList())
    }

    fun andQueries() {
        datastore.save(com.antwerkz.critter.test.Person("Mike", "Bloomberg"))
        datastore.save(com.antwerkz.critter.test.Person("Mike", "Tyson"))

        val query = datastore.createQuery<com.antwerkz.critter.test.Person>(com.antwerkz.critter.test.Person::class.java)
        query.and(
                query.criteria("first").equal("Mike"),
                query.criteria("last").equal("Tyson")
        )

        val criteria = PersonCriteria(datastore)
        criteria.and(
                criteria.first("Mike"),
                criteria.last("Tyson")
        )

        org.testng.Assert.assertEquals(criteria.query().asList().size, 1)
        org.testng.Assert.assertEquals(query.asList(), criteria.query().asList())
    }
}
