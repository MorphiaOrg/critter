/**
 * Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>
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
package com.antwerkz.critter.test;

import java.net.UnknownHostException;
import java.util.Date;

import com.antwerkz.critter.test.criteria.InvoiceCriteria;
import com.antwerkz.critter.test.criteria.PersonCriteria;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import org.joda.time.DateTime;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateResults;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

@Test
public class CriteriaTest {

  private Datastore datastore;

  @AfterMethod
  public void clean() {
    datastore = null;
  }

  public void invoice() {
    Datastore ds = getDatastore();
    Person john = new Person("John", "Doe");
    ds.save(john);
    ds.save(new Invoice(new DateTime(2012, 12, 21, 13, 15).toDate(), john, new Address("New York City", "NY", "10000"),
        new Item("ball", 5.0), new Item("skateboard", 17.35)));
    Person jeff = new Person("Jeff", "Johnson");
    ds.save(jeff);
    ds.save(new Invoice(new DateTime(2006, 3, 4, 8, 7).toDate(), jeff, new Address("Los Angeles", "CA", "90210"),
        new Item("movie", 29.95)));
    Person sally = new Person("Sally", "Ride");
    ds.save(sally);
    ds.save(new Invoice(new DateTime(2007, 8, 16, 19, 27).toDate(), sally, new Address("Chicago", "IL", "99999"),
        new Item("kleenex", 3.49), new Item("cough and cold syrup", 5.61)));
    InvoiceCriteria invoiceCriteria = new InvoiceCriteria(ds);
    invoiceCriteria.person(john);
    Invoice invoice = invoiceCriteria.query().get();
    Invoice doe = ds.createQuery(Invoice.class).filter("person =", john).get();
    Assert.assertEquals(invoice, doe);
    Assert.assertEquals(doe.getPerson().getLast(), "Doe");
    Assert.assertEquals(invoice.getPerson().getLast(), "Doe");
    Invoice query = ds.createQuery(Invoice.class).field("addresses.city").equal("Chicago").get();
    Assert.assertNotNull(query);
    invoiceCriteria = new InvoiceCriteria(ds);
    invoiceCriteria.addresses().city("Chicago");
    Invoice critter = invoiceCriteria.query().get();
    Assert.assertNotNull(critter);
    Assert.assertEquals(critter, query);
  }

  @Test
  public void updates() throws UnknownHostException {
    Datastore datastore = getDatastore();
    PersonCriteria personCriteria = new PersonCriteria(datastore);
    personCriteria.delete();
    personCriteria.first("Jim");
    personCriteria.last("Beam");

    Query<Person> query = personCriteria.query();

    Assert.assertEquals(personCriteria.getUpdater()
        .age(30L)
        .updateAll().getUpdatedCount(), 0);

    Assert.assertEquals(personCriteria.getUpdater()
        .age(30L)
        .upsert().getInsertedCount(), 1);

    UpdateResults update = personCriteria.getUpdater().incAge().updateAll();
    Assert.assertEquals(update.getUpdatedCount(), 1);
    Assert.assertEquals(personCriteria.query().get().getAge().longValue(), 31L);

    Assert.assertNotNull(new PersonCriteria(datastore).query().get().getFirst());

    WriteResult delete = datastore.delete(query);
    Assert.assertNull(delete.getError());
  }

  @Test
  public void updateFirst() {
    for (int i = 0; i < 100; i++) {
      getDatastore().save(new Person("First" + i, "Last" + i));
    }
    PersonCriteria criteria = new PersonCriteria(getDatastore());
    criteria.last().contains("Last2");
    criteria.getUpdater()
        .age(1000L)
        .updateFirst();

    criteria = new PersonCriteria(getDatastore());
    criteria.age(1000L);
    Assert.assertEquals(criteria.query().countAll(), 1);
  }

  public void embeds() {
    Invoice invoice = new Invoice();
    invoice.setDate(new Date());
    Person person = new Person("Mike", "Bloomberg");
    getDatastore().save(person);
    invoice.setPerson(person);
    invoice.add(new Address("New York City", "NY", "10036"));
    getDatastore().save(invoice);

    invoice = new Invoice();
    invoice.setDate(new Date());
    person = new Person("Andy", "Warhol");
    getDatastore().save(person);

    invoice.setPerson(person);
    invoice.add(new Address("NYC", "NY", "10018"));
    getDatastore().save(invoice);

    InvoiceCriteria criteria1 = new InvoiceCriteria(datastore);
    criteria1.addresses().city().order();
    Assert.assertEquals(criteria1.query().asList().get(0).getAddresses().get(0).getCity(), "NYC");

    InvoiceCriteria criteria2 = new InvoiceCriteria(datastore);
    criteria2.addresses().city().order(false);
    Assert.assertEquals(criteria2.query().asList().get(0).getAddresses().get(0).getCity(), "New York City");
  }

  public void orQueries() {
    getDatastore().save(new Person("Mike", "Bloomberg"));
    getDatastore().save(new Person("Mike", "Tyson"));

    final Query<Person> query = getDatastore().createQuery(Person.class);
    query.or(
        query.criteria("last").equal("Bloomberg"),
        query.criteria("last").equal("Tyson")
    );

    final PersonCriteria criteria = new PersonCriteria(getDatastore());
    criteria.or(
        criteria.last("Bloomberg"),
        criteria.last("Tyson")
    );

    Assert.assertEquals(criteria.query().asList().size(), 2);
    Assert.assertEquals(query.asList(), criteria.query().asList());
  }

  public void andQueries() {
    getDatastore().save(new Person("Mike", "Bloomberg"));
    getDatastore().save(new Person("Mike", "Tyson"));

    final Query<Person> query = getDatastore().createQuery(Person.class);
    query.and(
        query.criteria("first").equal("Mike"),
        query.criteria("last").equal("Tyson")
    );

    final PersonCriteria criteria = new PersonCriteria(getDatastore());
    criteria.and(
        criteria.first("Mike"),
        criteria.last("Tyson")
    );

    Assert.assertEquals(criteria.query().asList().size(), 1);
    Assert.assertEquals(query.asList(), criteria.query().asList());
  }

  private Datastore getDatastore() {
    if (datastore == null) {
      try {
        MongoClient mongo = new MongoClient();
        DB critter = mongo.getDB("critter");
        critter.dropDatabase();
        final Morphia morphia = new Morphia();
        morphia.mapPackage("com.antwerkz");
        datastore = morphia.createDatastore(mongo, "critter");
      } catch (UnknownHostException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return datastore;
  }
}
