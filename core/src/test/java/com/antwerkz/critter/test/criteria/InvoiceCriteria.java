package com.antwerkz.critter.test.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.antwerkz.critter.criteria.BaseCriteria;
import com.antwerkz.critter.test.Address;
import com.antwerkz.critter.test.Invoice;
import com.antwerkz.critter.test.Item;
import com.antwerkz.critter.test.Person;
import com.mongodb.WriteConcern;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

public class InvoiceCriteria extends BaseCriteria<Invoice> {
  private String prefix = "";

  public InvoiceCriteria(Datastore ds) {
    super(ds, Invoice.class);
  }


  // fields
  public TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.Date> date() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "date");
  }

  public Criteria date(java.util.Date value) {
    return new TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.Date>(this, query, prefix + "date").equal(value);
  }
  // end fields

  // fields
  public TypeSafeFieldEnd<InvoiceCriteria, Invoice, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "id");
  }

  public Criteria id(org.bson.types.ObjectId value) {
    return new TypeSafeFieldEnd<InvoiceCriteria, Invoice, org.bson.types.ObjectId>(this, query, prefix + "id").equal(value);
  }
  // end fields

  // fields
  public TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.List<com.antwerkz.critter.test.Item>> items() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "items");
  }

  public Criteria items(java.util.List<com.antwerkz.critter.test.Item> value) {
    return new TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.List<com.antwerkz.critter.test.Item>>(this, query, prefix + "items").equal(value);
  }
  // end fields

  // fields
  public TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.lang.Double> total() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "total");
  }

  public Criteria total(java.lang.Double value) {
    return new TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.lang.Double>(this, query, prefix + "total").equal(value);
  }
  // end fields


  public com.antwerkz.critter.test.criteria.AddressCriteria addresses() {
    return new com.antwerkz.critter.test.criteria.AddressCriteria(query, "addresses");
  }

  public InvoiceCriteria person(com.antwerkz.critter.test.Person reference) {
    query.filter("person = ", reference);
    return this;
  }

  public InvoiceUpdater getUpdater() {
    return new InvoiceUpdater();
  }

  public class InvoiceUpdater {
    UpdateOperations<Invoice> updateOperations;

    public InvoiceUpdater() {
      updateOperations = ds.createUpdateOperations(Invoice.class);
    }

    public UpdateResults update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults upsert(WriteConcern wc) {
      return ds.update(query(), updateOperations, true, wc);
    }

    // Updater Methods
    public InvoiceUpdater date(java.util.Date value) {
    updateOperations.set("date", value);
    return this;
    }

    public InvoiceUpdater unsetDate() {
    updateOperations.unset("date");
    return this;
    }


    public InvoiceUpdater items(java.util.List<com.antwerkz.critter.test.Item> value) {
    updateOperations.set("items", value);
    return this;
    }

    public InvoiceUpdater unsetItems() {
    updateOperations.unset("items");
    return this;
    }

      public InvoiceUpdater addItems(java.util.List<com.antwerkz.critter.test.Item> value) {
      updateOperations.add("items", value);
      return this;
      }

      public InvoiceUpdater addItems(java.util.List<com.antwerkz.critter.test.Item> value, boolean addDups) {
      updateOperations.add("items", value, addDups);
      return this;
      }

      public InvoiceUpdater addAllToItems(List<java.util.List<com.antwerkz.critter.test.Item>> values, boolean addDups) {
      updateOperations.addAll("items", values, addDups);
      return this;
      }

      public InvoiceUpdater removeFirstItems() {
      updateOperations.removeFirst("items");
      return this;
      }

      public InvoiceUpdater removeLastItems() {
      updateOperations.removeLast("items");
      return this;
      }

      public InvoiceUpdater removeFromItems(java.util.List<com.antwerkz.critter.test.Item> value) {
      updateOperations.removeAll("items", value);
      return this;
      }

      public InvoiceUpdater removeAllFromItems(List<java.util.List<com.antwerkz.critter.test.Item>> values) {
      updateOperations.removeAll("items", values);
      return this;
      }

    public InvoiceUpdater total(java.lang.Double value) {
    updateOperations.set("total", value);
    return this;
    }

    public InvoiceUpdater unsetTotal() {
    updateOperations.unset("total");
    return this;
    }


      public InvoiceUpdater decTotal() {
      updateOperations.dec("total");
      return this;
      }

      public InvoiceUpdater incTotal() {
      updateOperations.inc("total");
      return this;
      }

      public InvoiceUpdater incTotal(Number value) {
      updateOperations.inc("total", value);
      return this;
      }
    // Updater Methods
  }
}
