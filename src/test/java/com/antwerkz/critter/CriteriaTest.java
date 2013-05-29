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
package com.antwerkz.critter;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.antwerkz.critter.Invoice.Address;
import com.antwerkz.critter.criteria.InvoiceCriteria;
import com.antwerkz.critter.criteria.PersonCriteria;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class CriteriaTest {
  public void invoice() throws UnknownHostException {
    Datastore ds = getDatastore();
    Person john = new Person("John", "Doe");
    ds.save(john);
    ds.save(new Invoice(new Date(2012, 12, 21, 13, 15), john, new Address("New York City", "NY", "10000"),
        new Item("ball", 5.0), new Item("skateboard", 17.35)));
    Person jeff = new Person("Jeff", "Johnson");
    ds.save(jeff);
    ds.save(new Invoice(new Date(2006, 3, 4, 8, 7), jeff, new Address("Los Angeles", "CA", "90210"),
        new Item("movie", 29.95)));
    Person sally = new Person("Sally", "Ride");
    ds.save(sally);
    ds.save(new Invoice(new Date(2007, 8, 16, 19, 27), sally, new Address("Chicago", "IL", "99999"),
        new Item("kleenex", 3.49), new Item("cough and cold syrup", 5.61)));
    InvoiceCriteria invoiceCriteria = new InvoiceCriteria(ds);
    invoiceCriteria.person(john);
    Invoice invoice = invoiceCriteria.query().get();
    Invoice doe = ds.createQuery(Invoice.class).filter("person =", john).get();
    Assert.assertEquals(invoice, doe);
    Assert.assertEquals(doe.getPerson().getLast(), "Doe");
    Assert.assertEquals(invoice.getPerson().getLast(), "Doe");
    Invoice query = ds.createQuery(Invoice.class).field("address.city").equal("Chicago").get();
    Assert.assertNotNull(query);
    invoiceCriteria = new InvoiceCriteria(ds);
    invoiceCriteria.address().city("Chicago");
    Invoice critter = invoiceCriteria.query().get();
    Assert.assertNotNull(critter);
    Assert.assertEquals(critter, query);
  }

  public void updates() throws UnknownHostException {
    PersonCriteria criteria = new PersonCriteria(getDatastore());
    criteria.first("Jim");
    criteria.last("Beam");
    criteria.delete();

    Assert.assertEquals(criteria.getUpdater()
        .age(30L)
        .update().getUpdatedCount(), 0);
    Assert.assertEquals(criteria.getUpdater()
        .age(30L)
        .upsert().getInsertedCount(), 1);

    criteria.getUpdater().incAge().update();
    Assert.assertEquals(criteria.query().get().getAge().longValue(), 31L);

    Query<Person> query = criteria.query();
    System.out.println("query = " + query);
    WriteResult delete = getDatastore().delete(query);
    Assert.assertNull(delete.getError());
  }

  private Datastore getDatastore() throws UnknownHostException {
    Mongo mongo = new Mongo();
    DB critter = mongo.getDB("critter");
    critter.dropDatabase();
    Set<Class> classes = new HashSet<>();
    classes.add(Invoice.class);
    return new Morphia(classes).createDatastore(mongo, "critter");
  }
}
