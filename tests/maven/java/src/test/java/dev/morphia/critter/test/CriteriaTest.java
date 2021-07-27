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
package dev.morphia.critter.test;

import com.antwerkz.bottlerocket.BottleRocket;
import com.antwerkz.bottlerocket.BottleRocketTest;
import com.github.zafarkhaja.semver.Version;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.Morphia;
import dev.morphia.UpdateOptions;
import dev.morphia.critter.codec.CritterModelImporter;
import dev.morphia.critter.test.criteria.InvoiceCriteria;
import dev.morphia.critter.test.criteria.PersonCriteria;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.mongodb.WriteConcern.MAJORITY;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.or;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Test
public class CriteriaTest extends BottleRocketTest {

    @Test(dataProvider = "datastores")
    public void andQueries(String state, boolean useGenerated) {
        Datastore datastore = getDatastore(useGenerated);
        datastore.save(new Person("Mike", "Bloomberg"));
        datastore.save(new Person("Mike", "Tyson"));

        final Query<Person> query1 = datastore.find(Person.class)
                                              .filter(Filters.and(
                                                  PersonCriteria.firstName().eq("Mike"),
                                                  PersonCriteria.lastName().eq("Tyson")));

        final Query<Person> query2 = datastore.find(Person.class)
                                              .filter(
                                                  eq("firstName", "Mike"),
                                                  eq("lastName", "Tyson"));
        assertEquals(query2.iterator().toList().size(), 1);
        assertEquals(query1.iterator().toList(), query2.iterator().toList());
    }

    @BeforeMethod
    public void clean() {
        getDatabase().drop();
    }

    @NotNull
    @Override
    public String databaseName() {
        return "critter";
    }

    @Nullable
    @Override
    public Version version() {
        return BottleRocket.DEFAULT_VERSION;
    }

    @DataProvider(name = "datastores")
    public Object[][] datastores() {
        return new Object[][]{
            new Object[]{"Critter codecs", true},
            new Object[]{"Standard codecs", false},
            };
    }

    @Test(dataProvider = "datastores")
    public void embeds(String state, boolean useGenerated) {
        var datastore = getDatastore(useGenerated);
        Invoice invoice = new Invoice();
        invoice.setOrderDate(LocalDateTime.now());
        Person person = new Person("Mike", "Bloomberg");
        datastore.save(person);
        invoice.setPerson(person);
        invoice.add(new Address("New York City", "NY", "10036"));
        datastore.save(invoice);

        invoice = new Invoice();
        invoice.setOrderDate(LocalDateTime.now());
        person = new Person("Andy", "Warhol");
        datastore.save(person);

        invoice.setPerson(person);
        invoice.add(new Address("NYC", "NY", "10018"));
        datastore.save(invoice);

        MorphiaCursor<Invoice> criteria1 = datastore.find(Invoice.class)
                                                    .filter(InvoiceCriteria.orderDate().lte(LocalDateTime.now().plusDays(5)))
                                                    .iterator(new FindOptions()
                                                                  .sort(ascending(InvoiceCriteria.addresses().city().path())));
        List<Invoice> list = criteria1.toList();
        assertEquals(list.get(0).getAddresses().get(0).getCity(), "NYC");
        assertEquals(list.get(0), invoice);

        MorphiaCursor<Invoice> criteria2 = datastore.find(Invoice.class)
                                                    .iterator(new FindOptions()
                                                                  .sort(descending(InvoiceCriteria.addresses().city().path())));
        assertEquals(criteria2.toList().get(0).getAddresses().get(0).getCity(), "New York City");
    }

    @Test(dataProvider = "datastores")
    public void invoice(String state, boolean useGenerated) {
        Datastore ds = getDatastore(useGenerated);
        Person john = new Person("John", "Doe");
        ds.save(john);
        ds.save(new Invoice(LocalDateTime.of(2012, 12, 21, 13, 15), john, new Address("New York City", "NY", "10000"),
            new Item("ball", 5.0), new Item("skateboard", 17.35)));
        Person jeff = new Person("Jeff", "Johnson");
        ds.save(jeff);
        ds.save(new Invoice(LocalDateTime.of(2006, 3, 4, 8, 7), jeff, new Address("Los Angeles", "CA", "90210"),
            new Item("movie", 29.95)));
        Person sally = new Person("Sally", "Ride");
        ds.save(sally);
        ds.save(new Invoice(LocalDateTime.of(2007, 8, 16, 19, 27), sally, new Address("Chicago", "IL", "99999"),
            new Item("kleenex", 3.49), new Item("cough and cold syrup", 5.61)));
        Query<Invoice> query = ds.find(Invoice.class)
                                 .filter(InvoiceCriteria.person().eq(john));
        Invoice invoice = query.first();
        Invoice doe = ds.find(Invoice.class)
                        .filter(eq("person", john))
                        .first();

        assertNotNull(doe);
        assertEquals(invoice, doe);
        assertEquals(doe.getPerson().getLastName(), "Doe");

        assertNotNull(invoice);
        assertNotNull(invoice.getPerson());
        assertEquals(invoice.getPerson().getLastName(), "Doe");
        invoice = ds.find(Invoice.class)
                    .filter(eq("addresses.city", "Chicago"))
                    .first();
        assertNotNull(invoice);
        Invoice critter = ds.find(Invoice.class)
                            .filter(InvoiceCriteria.addresses().city().eq("Chicago"))
                            .first();
        assertNotNull(critter);
        assertEquals(critter, invoice);

        Invoice created = new Invoice(LocalDateTime.of(2012, 12, 21, 13, 15), john, new Address("New York City", "NY", "10000"),
            new Item("ball", 5.0), new Item("skateboard", 17.35));
        ds.save(created);
        assertTrue(created.isPrePersist());
        assertTrue(created.isPostPersist());
        assertFalse(created.isPreLoad());
        assertFalse(created.isPostLoad());

        Invoice loaded = ds.find(Invoice.class).filter(eq(InvoiceCriteria.id, created.getId())).first();
        assertFalse(loaded.isPrePersist());
        assertFalse(loaded.isPostPersist());
        assertTrue(loaded.isPreLoad());
        assertTrue(loaded.isPostLoad());
    }

    @Test(dataProvider = "datastores")
    public void orQueries(String state, boolean useGenerated) {
        Datastore datastore = getDatastore(useGenerated);
        datastore.save(new Person("Mike", "Bloomberg"));
        datastore.save(new Person("Mike", "Tyson"));

        final Query<Person> query = datastore.find(Person.class)
                                             .filter(or(
                                                 eq("lastName", "Bloomberg"),
                                                 eq("lastName", "Tyson")));

        final Query<Person> criteria = datastore.find(Person.class)
                                                .filter(or(
                                                    PersonCriteria.lastName().eq("Bloomberg"),
                                                    PersonCriteria.lastName().eq("Tyson")));

        assertEquals(criteria.count(), 2);
        assertEquals(query.iterator().toList(), criteria.iterator().toList());
    }

    public void paths() {
        assertEquals(InvoiceCriteria.addresses().city().path(), "addresses.city");
        assertEquals(InvoiceCriteria.orderDate().path(), "orderDate");
    }

    @Test(dataProvider = "datastores")
    public void removes(String state, boolean useGenerated) {
        Datastore datastore = getDatastore(useGenerated);
        for (int i = 0; i < 100; i++) {
            datastore.save(new Person("First" + i, "Last" + i));
        }
        Query<Person> criteria = datastore.find(Person.class)
                                          .filter(PersonCriteria.lastName().regex()
                                                                .pattern("Last2"));
        DeleteResult result = criteria.delete(new DeleteOptions()
                                                  .multi(true));
        assertEquals(result.getDeletedCount(), 11);
        assertEquals(criteria.count(), 0);

        criteria = datastore.find(Person.class);
        assertEquals(criteria.count(), 89);

        criteria = datastore.find(Person.class)
                            .filter(PersonCriteria.lastName().regex()
                                                  .pattern("Last3"));
        result = criteria.delete(new DeleteOptions()
                                     .multi(true)
                                     .writeConcern(MAJORITY));
        assertEquals(result.getDeletedCount(), 11);

        assertEquals(criteria.count(), 0);
    }

    @Test(dataProvider = "datastores")
    public void updateFirst(String state, boolean useGenerated) {
        Datastore datastore = getDatastore(useGenerated);
        for (int i = 0; i < 100; i++) {
            datastore.save(new Person("First" + i, "Last" + i));
        }
        Query<Person> query = datastore.find(Person.class)
                                       .filter(PersonCriteria.lastName().regex()
                                                             .pattern("Last2"));

        query.update(PersonCriteria.age().set(1000L))
             .execute();

        query = datastore.find(Person.class)
                         .filter(PersonCriteria.age().eq(1000L));

        assertEquals(query.count(), 1L);
    }

    @Test(dataProvider = "datastores")
    public void updates(String state, boolean useGenerated) {
        Datastore datastore = getDatastore(useGenerated);
        Query<Person> query = datastore.find(Person.class);
        query.delete();

        query.filter(
            PersonCriteria.firstName().eq("Jim"),
            PersonCriteria.lastName().eq("Beam"));

        assertEquals(query.update(
            PersonCriteria.age().set(30L))
                          .execute(new UpdateOptions().multi(true))
                          .getModifiedCount(), 0);

        assertNotNull(query.update(PersonCriteria.age().set(30L))
                           .execute(new UpdateOptions().upsert(true))
                           .getUpsertedId());

        final UpdateResult update = query.update(PersonCriteria.age().inc())
                                         .execute(new UpdateOptions().multi(true));
        assertEquals(update.getModifiedCount(), 1);
        assertEquals(datastore.find(Person.class)
                              .first().getAge().longValue(), 31L);

        assertNotNull(datastore.find(Person.class).first().getFirstName());

        DeleteResult delete = datastore.find(Person.class).delete();
        assertEquals(delete.getDeletedCount(), 1);
    }

    private Datastore getDatastore(boolean useGenerated) {
        var datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName());
        if (useGenerated) {
            datastore.getMapper().importModels(new CritterModelImporter());
        }
        return datastore;
    }
}
