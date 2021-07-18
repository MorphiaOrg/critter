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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static com.mongodb.WriteConcern.MAJORITY;
import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.or;

@Test
public class CriteriaTest extends BottleRocketTest {

    private Datastore datastore;

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

    public void paths() {
        Assert.assertEquals(
            InvoiceCriteria.addresses().city().path(),
            "addresses.city");
        Assert.assertEquals(
            InvoiceCriteria.orderDate().path(),
            "orderDate");
    }

    public void andQueries() {
        getDatastore().save(new Person("Mike", "Bloomberg"));
        getDatastore().save(new Person("Mike", "Tyson"));

        final Query<Person> query1 = getDatastore().find(Person.class)
                                                   .filter(Filters.and(
                                                       PersonCriteria.firstName().eq("Mike"),
                                                       PersonCriteria.lastName().eq("Tyson")));

        final Query<Person> query2 = getDatastore().find(Person.class)
                                                   .filter(
                                                       eq("firstName", "Mike"),
                                                       eq("lastName", "Tyson"));
        Assert.assertEquals(query2.iterator().toList().size(), 1);
        Assert.assertEquals(query1.iterator().toList(), query2.iterator().toList());
    }

    @AfterMethod
    public void clean() {
        datastore = null;
    }

    public void embeds() {
        Invoice invoice = new Invoice();
        invoice.setOrderDate(LocalDateTime.now());
        Person person = new Person("Mike", "Bloomberg");
        getDatastore().save(person);
        invoice.setPerson(person);
        invoice.add(new Address("New York City", "NY", "10036"));
        getDatastore().save(invoice);

        invoice = new Invoice();
        invoice.setOrderDate(LocalDateTime.now());
        person = new Person("Andy", "Warhol");
        getDatastore().save(person);

        invoice.setPerson(person);
        invoice.add(new Address("NYC", "NY", "10018"));
        getDatastore().save(invoice);

        MorphiaCursor<Invoice> criteria1 = datastore.find(Invoice.class)
                                                    .filter(InvoiceCriteria.orderDate().lte(LocalDateTime.now().plusDays(5)))
                                                    .iterator(new FindOptions()
                                                                  .sort(ascending(InvoiceCriteria.addresses().city().path())));
        Assert.assertEquals(criteria1.toList().get(0).getAddresses().get(0).getCity(), "NYC");

        MorphiaCursor<Invoice> criteria2 = datastore.find(Invoice.class)
                                                    .iterator(new FindOptions()
                                                                  .sort(descending(InvoiceCriteria.addresses().city().path())));
        Assert.assertEquals(criteria2.toList().get(0).getAddresses().get(0).getCity(), "New York City");
    }

    public void invoice() {
        Datastore ds = getDatastore();
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

        Assert.assertNotNull(doe);
        Assert.assertEquals(invoice, doe);
        Assert.assertEquals(doe.getPerson().getLastName(), "Doe");

        Assert.assertNotNull(invoice);
        Assert.assertNotNull(invoice.getPerson());
        Assert.assertEquals(invoice.getPerson().getLastName(), "Doe");
        invoice = ds.find(Invoice.class)
                    .filter(eq("addresses.city", "Chicago"))
                    .first();
        Assert.assertNotNull(invoice);
        Invoice critter = ds.find(Invoice.class)
                            .filter(InvoiceCriteria.addresses().city().eq("Chicago"))
                            .first();
        Assert.assertNotNull(critter);
        Assert.assertEquals(critter, invoice);
    }

    public void orQueries() {
        getDatastore().save(new Person("Mike", "Bloomberg"));
        getDatastore().save(new Person("Mike", "Tyson"));

        final Query<Person> query = getDatastore().find(Person.class)
                                                  .filter(or(
                                                          eq("lastName", "Bloomberg"),
                                                          eq("lastName", "Tyson")));

        final Query<Person> criteria = getDatastore().find(Person.class)
                                                     .filter(or(
                                                         PersonCriteria.lastName().eq("Bloomberg"),
                                                         PersonCriteria.lastName().eq("Tyson")));

        Assert.assertEquals(criteria.count(), 2);
        Assert.assertEquals(query.iterator().toList(), criteria.iterator().toList());
    }

    @Test
    public void removes() {
        for (int i = 0; i < 100; i++) {
            getDatastore().save(new Person("First" + i, "Last" + i));
        }
        Query<Person> criteria = datastore.find(Person.class)
                                          .filter(PersonCriteria.lastName().regex()
                                                                .pattern("Last2"));
        DeleteResult result = criteria.delete(new DeleteOptions()
                                             .multi(true));
        Assert.assertEquals(result.getDeletedCount(), 11);
        Assert.assertEquals(criteria.count(), 0);

        criteria = datastore.find(Person.class);
        Assert.assertEquals(criteria.count(), 89);

        criteria = datastore.find(Person.class)
                            .filter(PersonCriteria.lastName().regex()
                                                  .pattern("Last3"));
        result = criteria.delete(new DeleteOptions()
                                     .multi(true)
                                     .writeConcern(MAJORITY));
        Assert.assertEquals(result.getDeletedCount(), 11);

        Assert.assertEquals(criteria.count(), 0);
    }

    @Test
    public void updateFirst() {
        for (int i = 0; i < 100; i++) {
            getDatastore().save(new Person("First" + i, "Last" + i));
        }
        Query<Person> query = datastore.find(Person.class)
                                       .filter(PersonCriteria.lastName().regex()
                                                             .pattern("Last2"));

        query.update(PersonCriteria.age().set(1000L))
             .execute();

        query = datastore.find(Person.class)
                         .filter(PersonCriteria.age().eq(1000L));

        Assert.assertEquals(query.count(), 1L);
    }

    @Test
    public void updates() {
        Datastore datastore = getDatastore();
        Query<Person> query = datastore.find(Person.class);
        query.delete();

        query.filter(
            PersonCriteria.firstName().eq("Jim"),
            PersonCriteria.lastName().eq("Beam"));

        Assert.assertEquals(query.update(
            PersonCriteria.age().set(30L))
                                 .execute(new UpdateOptions().multi(true))
                                 .getModifiedCount(), 0);

        Assert.assertNotNull(query.update(PersonCriteria.age().set(30L))
                                  .execute(new UpdateOptions().upsert(true))
                                  .getUpsertedId());

        final UpdateResult update = query.update(PersonCriteria.age().inc())
                                         .execute(new UpdateOptions().multi(true));
        Assert.assertEquals(update.getModifiedCount(), 1);
        Assert.assertEquals(datastore.find(Person.class)
                                     .first().getAge().longValue(), 31L);

        Assert.assertNotNull(datastore.find(Person.class).first().getFirstName());

        DeleteResult delete = datastore.find(Person.class).delete();
        Assert.assertEquals(delete.getDeletedCount(), 1);
    }

    private Datastore getDatastore() {
        if (datastore == null) {
            MongoClient mongo = getMongoClient();
            MongoDatabase critter = getDatabase();
            critter.drop();
            datastore = Morphia.createDatastore(mongo, getDatabase().getName());
            datastore.getMapper().importModels(new CritterModelImporter());
        }
        return datastore;
    }
}
