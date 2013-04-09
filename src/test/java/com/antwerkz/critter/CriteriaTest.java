package com.antwerkz.critter;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.antwerkz.critter.Invoice.Address;
import com.antwerkz.critter.Invoice.Person;
import com.antwerkz.critter.criteria.InvoiceCriteria;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;
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
    ds.save(new Invoice(new Date(2007, 8, 16, 19, 27), sally,new Address("Chicago", "IL", "99999"),
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

  private Datastore getDatastore() throws UnknownHostException {
    Mongo mongo = new Mongo();
    DB critter = mongo.getDB("critter");
    critter.dropDatabase();
    Set<Class> classes = new HashSet<>();
    classes.add(Invoice.class);
    return new Morphia(classes).createDatastore(mongo, "critter");
  }
}
