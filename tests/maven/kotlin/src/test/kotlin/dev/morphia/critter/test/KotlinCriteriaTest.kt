/*
 * Copyright (C) 2012-2020 Justin Lee <jlee@antwerkz.com>
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
package dev.morphia.critter.test

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern.MAJORITY
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import dev.morphia.Datastore
import dev.morphia.DeleteOptions
import dev.morphia.Morphia
import dev.morphia.UpdateOptions
import dev.morphia.critter.test.criteria.InvoiceCriteria
import dev.morphia.critter.test.criteria.InvoiceCriteria.Companion.addresses
import dev.morphia.critter.test.criteria.InvoiceCriteria.Companion.orderDate
import dev.morphia.critter.test.criteria.PersonCriteria.Companion.age
import dev.morphia.critter.test.criteria.PersonCriteria.Companion.first
import dev.morphia.critter.test.criteria.PersonCriteria.Companion.last
import dev.morphia.mapping.MapperOptions
import dev.morphia.query.FindOptions
import dev.morphia.query.MorphiaCursor
import dev.morphia.query.Sort
import dev.morphia.query.Sort.ascending
import dev.morphia.query.filters.Filters
import dev.morphia.query.filters.Filters.and
import java.time.LocalDateTime.now
import java.time.LocalDateTime.of
import java.util.stream.Collectors
import org.bson.UuidRepresentation.STANDARD
import org.testcontainers.containers.MongoDBContainer
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertTrue
import org.testng.Assert.fail
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeTest
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Test
@Suppress("UNUSED_PARAMETER", "removal", "DEPRECATION")
class KotlinCriteriaTest {
    companion object {
        var mongoDBContainer: MongoDBContainer? = null
        lateinit var  database: MongoDatabase
        lateinit var mongoClient: MongoClient
        lateinit var datastore: Datastore

        @BeforeTest
        fun setup() {
            mongoDBContainer = MongoDBContainer("mongo:6")
            mongoDBContainer?.let {
                it.start()

                mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                        .uuidRepresentation(STANDARD)
                        .applyConnectionString(ConnectionString(it.replicaSetUrl))
                        .build()
                )

                datastore = Morphia.createDatastore(mongoClient, "test")
                database = datastore.database
            }
        }

        @AfterTest
        fun shutdown() {
            mongoDBContainer?.close()
        }
    }

    @Test(dataProvider = "datastores")
    fun andQueries(state: String, datastore: Datastore) {
        datastore.save(Person("Mike", "Bloomberg"))
        datastore.save(Person("Mike", "Tyson"))
        val query1 = datastore.find(Person::class.java).filter(
            Filters.eq(first, "Mike"), Filters.eq(last, "Tyson")
        )
        val query2 = datastore.find(Person::class.java).filter(
            and(
                first().eq("Mike"), last().eq("Tyson")
            )
        )
        assertEquals(query2.toList().size, 1)
        assertEquals(query1.toList(), query2.toList())
    }

    @BeforeMethod
    fun clean() {
        database.drop()
    }

    @DataProvider(name = "datastores")
    fun datastores(): Array<Array<Any>> {
        return arrayOf(
            arrayOf("Standard codecs", getDatastore(false)), arrayOf("Critter codecs", getDatastore(true))
        )
    }

    @Test(dataProvider = "datastores")
    fun embeds(state: String, datastore: Datastore) {
        datastore.mapper.map(Invoice::class.java)
        var person = Person("Mike", "Bloomberg")
        datastore.save(person)
        var invoice = Invoice(now(), person, Address("New York City", "NY", "10036"))
        datastore.save(invoice)
        person = Person("Andy", "Warhol")
        datastore.save(person)
        invoice = Invoice(now(), person, Address("NYC", "NY", "10018"))
        datastore.save(invoice)
        val criteria1: MorphiaCursor<Invoice> = datastore.find(
            Invoice::class.java
        ).filter(orderDate().lte(now().plusDays(5))).iterator(
                FindOptions().sort(ascending(addresses().city().path))
            )
        val list = criteria1.toList()
        assertEquals(
            list[0].addresses[0].city, "NYC", list.stream().map { obj: Invoice -> obj.id }.collect(
                Collectors.toList()
            ).toString()
        )
        assertEquals(list[0], invoice, list.stream().map { obj: Invoice -> obj.id }.toString())
        val criteria2: MorphiaCursor<Invoice> = datastore.find(
            Invoice::class.java
        ).iterator(
                FindOptions().sort(Sort.descending(addresses().city().path))
            )
        assertEquals(criteria2.toList()[0].addresses[0].city, "New York City")
    }

    private fun getDatastore(useGenerated: Boolean): Datastore {
        return Morphia.createDatastore(
            mongoClient, database.name, MapperOptions.builder().autoImportModels(useGenerated).build()
        )
    }

    @Test(dataProvider = "datastores")
    fun invoice(state: String, ds: Datastore) {
        assertTrue(!ds.mapper.options.autoImportModels() xor ds.mapper.isMapped(Invoice::class.java))
        val john = Person("John", "Doe")
        ds.save(john)
        ds.save(
            Invoice(
                of(2012, 12, 21, 13, 15),
                john,
                Address("New York City", "NY", "10000"),
                Item("ball", 5.0),
                Item("skateboard", 17.35)
            )
        )
        val jeff = Person("Jeff", "Johnson")
        ds.save(jeff)
        ds.save(
            Invoice(
                of(2006, 3, 4, 8, 7), jeff, Address("Los Angeles", "CA", "90210"), Item("movie", 29.95)
            )
        )
        val sally = Person("Sally", "Ride")
        ds.save(sally)
        ds.save(
            Invoice(
                of(2007, 8, 16, 19, 27),
                sally,
                Address("Chicago", "IL", "99999"),
                Item("kleenex", 3.49),
                Item("cough and cold syrup", 5.61)
            )
        )
        val query = ds.find(Invoice::class.java).filter(InvoiceCriteria.person().eq(john))
        var invoice = query.first() as Invoice
        val doe = ds.find(Invoice::class.java).filter(Filters.eq(InvoiceCriteria.person, john)).first() as Invoice
        assertNotNull(doe)
        assertEquals(invoice, doe)
        assertEquals(doe.person?.last, "Doe")
        assertNotNull(invoice)
        assertNotNull(invoice.person)
        assertEquals(invoice.person?.last, "Doe")
        invoice = ds.find(Invoice::class.java).filter(Filters.eq(addresses().city().path, "Chicago")).first() as Invoice
        assertNotNull(invoice)
        val critter = ds.find(Invoice::class.java).filter(addresses().city().eq("Chicago")).first()
        assertNotNull(critter)
        assertEquals(critter, invoice)
        val created = Invoice(
            of(2012, 12, 21, 13, 15),
            john,
            Address("New York City", "NY", "10000"),
            Item("ball", 5.0),
            Item("skateboard", 17.35)
        )
        ds.save(created)
        assertTrue(created.isPrePersist)
        assertTrue(created.isPostPersist)
        assertFalse(created.isPreLoad)
        assertFalse(created.isPostLoad)
        val loaded = ds.find(Invoice::class.java).filter(Filters.eq(InvoiceCriteria.id, created.id)).first() as Invoice
        assertFalse(loaded.isPrePersist)
        assertFalse(loaded.isPostPersist)
        assertTrue(loaded.isPreLoad)
        assertTrue(loaded.isPostLoad)
    }

    @Test(dataProvider = "datastores")
    fun orQueries(state: String, datastore: Datastore) {
        datastore.save(Person("Mike", "Bloomberg"))
        datastore.save(Person("Mike", "Tyson"))
        val query = datastore.find(Person::class.java).filter(
                Filters.or(
                    Filters.eq("last", "Bloomberg"), Filters.eq("last", "Tyson")
                )
            )
        val criteria = datastore.find(Person::class.java).filter(
                Filters.or(
                    last().eq("Bloomberg"), last().eq("Tyson")
                )
            )
        assertEquals(criteria.count(), 2)
        assertEquals(query.iterator().toList(), criteria.iterator().toList())
    }

    fun paths() {
        assertEquals(addresses().city().path, "addresses.city")
        assertEquals(orderDate().path, "orderDate")
    }

    @Test(dataProvider = "datastores")
    fun removes(state: String, datastore: Datastore) {
        for (i in 0..99) {
            datastore.save(Person("First$i", "Last$i"))
        }
        var criteria = datastore.find(Person::class.java).filter(
                last().regex().pattern("Last2")
            )
        var result = criteria.delete(
            DeleteOptions().multi(true)
        )
        assertEquals(result.deletedCount, 11)
        assertEquals(criteria.count(), 0)
        criteria = datastore.find(Person::class.java)
        assertEquals(criteria.count(), 89)
        criteria = datastore.find(Person::class.java).filter(
                last().regex().pattern("Last3")
            )
        result = criteria.delete(
            DeleteOptions().multi(true).writeConcern(MAJORITY)
        )
        assertEquals(result.deletedCount, 11)
        assertEquals(criteria.count(), 0)
    }

    @Test(dataProvider = "datastores")
    fun updateFirst(state: String, datastore: Datastore) {
        for (i in 0..99) {
            datastore.save(Person("First$i", "Last$i"))
        }
        var query = datastore.find(Person::class.java).filter(
                last().regex().pattern("Last2")
            )
        query.update(age().set(1000L)).execute()
        query = datastore.find(Person::class.java).filter(age().eq(1000L))
        assertEquals(query.count(), 1L)
    }

    @Test(dataProvider = "datastores")
    fun updates(state: String, datastore: Datastore) {
        val query = datastore.find(Person::class.java)
        query.delete()
        query.filter(
            first().eq("Jim"), last().eq("Beam")
        )
        assertEquals(
            query.update(
                age().set(30L)
            ).execute(UpdateOptions().multi(true)).modifiedCount, 0
        )
        assertNotNull(
            query.update(age().set(30L)).execute(UpdateOptions().upsert(true)).upsertedId
        )
        val update = query.update(age().inc()).execute(UpdateOptions().multi(true))
        assertEquals(update.modifiedCount, 1)
        assertEquals(
            datastore.find(Person::class.java).first()?.age, 31L
        )
        assertNotNull(datastore.find(Person::class.java).first()?.first)
        val delete = datastore.find(Person::class.java).delete()
        assertEquals(delete.deletedCount, 1)
    }
}
