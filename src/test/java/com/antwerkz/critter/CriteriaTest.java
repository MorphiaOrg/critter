package com.antwerkz.critter;

import com.antwerkz.critter.Invoice.Person;
import com.antwerkz.critter.criteria.InvoiceCriteria;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Test
public class CriteriaTest {
  public void invoice() throws UnknownHostException {
    Datastore ds = getDatastore();
    ds.save(new Invoice(new Date(2012, 12, 21, 13, 15), new Person("John", "Doe"),
      new Item("ball", 5.0), new Item("skateboard", 17.35)));
    ds.save(new Invoice(new Date(2006, 3, 4, 8, 7), new Person("Jeff", "Johnson"), new Item("movie", 29.95)));
    ds.save(new Invoice(new Date(2007, 8, 16, 19, 27), new Person("Sally", "Ride"),
      new Item("kleenex", 3.49), new Item("cough and cold syrup", 5.61)));

    Invoice name = ds.createQuery(Invoice.class).field("person.first").equal("John").get();
    InvoiceCriteria invoiceCriteria = new InvoiceCriteria(ds);
    invoiceCriteria.person().first().equal("John");
    Invoice invoice = invoiceCriteria.query().get();
    Assert.assertEquals(name, invoice);
    Assert.assertEquals(name.getPerson().getLast(), "Doe");

    Invoice person = ds.createQuery(Invoice.class).field("person.first").equal("John").get();

    Assert.assertNotNull(person);
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
