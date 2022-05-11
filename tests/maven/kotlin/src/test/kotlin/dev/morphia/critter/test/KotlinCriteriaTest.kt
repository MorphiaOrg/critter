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

import com.antwerkz.bottlerocket.BottleRocket.DEFAULT_VERSION
import com.antwerkz.bottlerocket.BottleRocketTest
import com.github.zafarkhaja.semver.Version
import com.mongodb.WriteConcern.MAJORITY
import dev.morphia.Datastore
import dev.morphia.DeleteOptions
import dev.morphia.Morphia
import dev.morphia.UpdateOptions
import dev.morphia.critter.test.criteria.InvoiceCriteria
import dev.morphia.critter.test.criteria.InvoiceCriteria.Companion.addresses
import dev.morphia.critter.test.criteria.InvoiceCriteria.Companion.orderDate
import dev.morphia.critter.test.criteria.PersonCriteria
import dev.morphia.critter.test.criteria.UserCriteria.Companion.age
import dev.morphia.mapping.MapperOptions
import dev.morphia.query.FindOptions
import dev.morphia.query.MorphiaCursor
import dev.morphia.query.Sort
import dev.morphia.query.Sort.ascending
import dev.morphia.query.filters.Filters
import dev.morphia.query.filters.Filters.and
import org.testng.Assert
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.stream.Collectors

@Test
@Suppress("UNUSED_PARAMETER")
class KotlinCriteriaTest : BottleRocketTest() {
    override fun databaseName(): String {
        return "critter"
    }

    override fun version(): Version {
        return DEFAULT_VERSION
    }

    @Test(dataProvider = "datastores")
    fun andQueries(state: String, datastore: Datastore) {
        datastore.save(Person("Mike", "Bloomberg"))
        datastore.save(Person("Mike", "Tyson"))
        val query1 = datastore.find(Person::class.java).filter(
                and(
                    PersonCriteria.first().eq("Mike"), PersonCriteria.last().eq("Tyson")
                )
            )
        val query2 = datastore.find(Person::class.java).filter(
                Filters.eq(PersonCriteria.first, "Mike"), Filters.eq(PersonCriteria.last, "Tyson")
            )
        Assert.assertEquals(query2.iterator().toList().size, 1)
        Assert.assertEquals(query1.iterator().toList(), query2.iterator().toList())
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
        Assert.assertEquals(
            list[0].addresses[0].city, "NYC", list.stream().map { obj: Invoice -> obj.id }.collect(
                Collectors.toList()
            ).toString()
        )
        Assert.assertEquals(list[0], invoice, list.stream().map { obj: Invoice -> obj.id }.toString())
        val criteria2: MorphiaCursor<Invoice> = datastore.find(
            Invoice::class.java
        ).iterator(
                FindOptions().sort(Sort.descending(addresses().city().path))
            )
        Assert.assertEquals(criteria2.toList()[0].addresses[0].city, "New York City")
    }

    @Test(dataProvider = "datastores")
    fun invoice(state: String, ds: Datastore) {
        assertTrue(!ds.mapper.options.isAutoImportModels xor ds.mapper.isMapped(Invoice::class.java))
        val john = Person("John", "Doe")
        ds.save(john)
        ds.save(
            Invoice(
                LocalDateTime.of(2012, 12, 21, 13, 15),
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
                LocalDateTime.of(2006, 3, 4, 8, 7), jeff, Address("Los Angeles", "CA", "90210"), Item("movie", 29.95)
            )
        )
        val sally = Person("Sally", "Ride")
        ds.save(sally)
        ds.save(
            Invoice(
                LocalDateTime.of(2007, 8, 16, 19, 27),
                sally,
                Address("Chicago", "IL", "99999"),
                Item("kleenex", 3.49),
                Item("cough and cold syrup", 5.61)
            )
        )
        val query = ds.find(Invoice::class.java).filter(InvoiceCriteria.person().eq(john))
        var invoice = query.first() as Invoice
        val doe = ds.find(Invoice::class.java).filter(Filters.eq(InvoiceCriteria.person, john)).first() as Invoice
        Assert.assertNotNull(doe)
        Assert.assertEquals(invoice, doe)
        Assert.assertEquals(doe.person?.last, "Doe")
        Assert.assertNotNull(invoice)
        Assert.assertNotNull(invoice.person)
        Assert.assertEquals(invoice.person?.last, "Doe")
        invoice = ds.find(Invoice::class.java).filter(Filters.eq(addresses().city().path, "Chicago")).first() as Invoice
        Assert.assertNotNull(invoice)
        val critter = ds.find(Invoice::class.java).filter(addresses().city().eq("Chicago")).first()
        Assert.assertNotNull(critter)
        Assert.assertEquals(critter, invoice)
        val created = Invoice(
            LocalDateTime.of(2012, 12, 21, 13, 15),
            john,
            Address("New York City", "NY", "10000"),
            Item("ball", 5.0),
            Item("skateboard", 17.35)
        )
        ds.save(created)
        assertTrue(created.isPrePersist)
        assertTrue(created.isPostPersist)
        Assert.assertFalse(created.isPreLoad)
        Assert.assertFalse(created.isPostLoad)
        val loaded = ds.find(Invoice::class.java).filter(Filters.eq(InvoiceCriteria.id, created.id)).first() as Invoice
        Assert.assertFalse(loaded.isPrePersist)
        Assert.assertFalse(loaded.isPostPersist)
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
                    PersonCriteria.last().eq("Bloomberg"), PersonCriteria.last().eq("Tyson")
                )
            )
        Assert.assertEquals(criteria.count(), 2)
        Assert.assertEquals(query.iterator().toList(), criteria.iterator().toList())
    }

    fun paths() {
        Assert.assertEquals(addresses().city().path, "addresses.city")
        Assert.assertEquals(orderDate().path, "orderDate")
    }

    @Test(dataProvider = "datastores")
    fun removes(state: String, datastore: Datastore) {
        for (i in 0..99) {
            datastore.save(Person("First$i", "Last$i"))
        }
        var criteria = datastore.find(Person::class.java).filter(
                PersonCriteria.last().regex().pattern("Last2")
            )
        var result = criteria.delete(
            DeleteOptions().multi(true)
        )
        Assert.assertEquals(result.deletedCount, 11)
        Assert.assertEquals(criteria.count(), 0)
        criteria = datastore.find(Person::class.java)
        Assert.assertEquals(criteria.count(), 89)
        criteria = datastore.find(Person::class.java).filter(
                PersonCriteria.last().regex().pattern("Last3")
            )
        result = criteria.delete(
            DeleteOptions().multi(true).writeConcern(MAJORITY)
        )
        Assert.assertEquals(result.deletedCount, 11)
        Assert.assertEquals(criteria.count(), 0)
    }

    @Test(dataProvider = "datastores")
    fun updateFirst(state: String, datastore: Datastore) {
        for (i in 0..99) {
            datastore.save(Person("First$i", "Last$i"))
        }
        var query = datastore.find(Person::class.java).filter(
                PersonCriteria.last().regex().pattern("Last2")
            )
        query.update(age().set(1000L)).execute()
        query = datastore.find(Person::class.java).filter(age().eq(1000L))
        Assert.assertEquals(query.count(), 1L)
    }

    @Test(dataProvider = "datastores")
    fun updates(state: String, datastore: Datastore) {
        val query = datastore.find(Person::class.java)
        query.delete()
        query.filter(
            PersonCriteria.first().eq("Jim"), PersonCriteria.last().eq("Beam")
        )
        Assert.assertEquals(
            query.update(
                age().set(30L)
            ).execute(UpdateOptions().multi(true)).modifiedCount, 0
        )
        Assert.assertNotNull(
            query.update(age().set(30L)).execute(UpdateOptions().upsert(true)).upsertedId
        )
        val update = query.update(age().inc()).execute(UpdateOptions().multi(true))
        Assert.assertEquals(update.modifiedCount, 1)
        Assert.assertEquals(
            datastore.find(Person::class.java).first()?.age, 31L
        )
        Assert.assertNotNull(datastore.find(Person::class.java)?.first()?.first)
        val delete = datastore.find(Person::class.java).delete()
        Assert.assertEquals(delete.deletedCount, 1)
    }

    private fun getDatastore(useGenerated: Boolean): Datastore {
        return Morphia.createDatastore(
            mongoClient, database.name, MapperOptions.builder().autoImportModels(useGenerated).build()
        )
    }
}
