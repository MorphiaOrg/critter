package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Invoice;

public class InvoiceCriteria extends com.antwerkz.critter.criteria.BaseCriteria<Invoice> {
  private String prefix = "";

  public InvoiceCriteria(org.mongodb.morphia.Datastore ds) {
    super(ds, Invoice.class);
  }


  public com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.Date> date() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "date");
  }

  public InvoiceCriteria date(java.util.Date value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.Date>(this, query, prefix + "date").equal(value);
    return this;
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, org.bson.types.ObjectId> id() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "id");
  }

  public InvoiceCriteria id(org.bson.types.ObjectId value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, org.bson.types.ObjectId>(this, query, prefix + "id").equal(value);
    return this;
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.List<com.antwerkz.critter.Item>> items() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "items");
  }

  public InvoiceCriteria items(java.util.List<com.antwerkz.critter.Item> value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.util.List<com.antwerkz.critter.Item>>(this, query, prefix + "items").equal(value);
    return this;
  }

  public com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.lang.Double> total() {
    return new com.antwerkz.critter.TypeSafeFieldEnd<>(this, query, prefix + "total");
  }

  public InvoiceCriteria total(java.lang.Double value) {
    new com.antwerkz.critter.TypeSafeFieldEnd<InvoiceCriteria, Invoice, java.lang.Double>(this, query, prefix + "total").equal(value);
    return this;
  }


  public com.antwerkz.critter.criteria.AddressCriteria addresses() {
    return new com.antwerkz.critter.criteria.AddressCriteria(query, "addresses");
  }

  public InvoiceCriteria person(com.antwerkz.critter.Person reference) {
    query.filter("person = ", reference);
    return this;
  }

  public InvoiceUpdater getUpdater() {
    return new InvoiceUpdater();
  }

  public class InvoiceUpdater {
    org.mongodb.morphia.query.UpdateOperations<Invoice> updateOperations;

    public InvoiceUpdater() {
      updateOperations = ds.createUpdateOperations(Invoice.class);
    }

    public org.mongodb.morphia.query.UpdateResults<Invoice> update() {
      return ds.update(query(), updateOperations, false);
    }

    public org.mongodb.morphia.query.UpdateResults<Invoice> update(com.mongodb.WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public org.mongodb.morphia.query.UpdateResults<Invoice> upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public org.mongodb.morphia.query.UpdateResults<Invoice> upsert(com.mongodb.WriteConcern wc) {
      return ds.update(query(), updateOperations, true, wc);
    }

    public InvoiceUpdater date(java.util.Date value) {
      updateOperations.set("date", value);
      return this;
    }

    public InvoiceUpdater unsetDate(java.util.Date value) {
      updateOperations.unset("date");
      return this;
    }

    public InvoiceUpdater addDate(java.util.Date value) {
      updateOperations.add("date", value);
      return this;
    }

    public InvoiceUpdater addDate(java.util.Date value, boolean addDups) {
      updateOperations.add("date", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToDate(java.util.List<java.util.Date> values, boolean addDups) {
      updateOperations.addAll("date", values, addDups);
      return this;
    }
  
    public InvoiceUpdater removeFirstDate() {
      updateOperations.removeFirst("date");
      return this;
    }
  
    public InvoiceUpdater removeLastDate() {
      updateOperations.removeLast("date");
      return this;
    }
  
    public InvoiceUpdater removeFromDate(java.util.Date value) {
      updateOperations.removeAll("date", value);
      return this;
    }

    public InvoiceUpdater removeAllFromDate(java.util.List<java.util.Date> values) {
      updateOperations.removeAll("date", values);
      return this;
    }
 
    public InvoiceUpdater decDate() {
      updateOperations.dec("date");
      return this;
    }

    public InvoiceUpdater incDate() {
      updateOperations.inc("date");
      return this;
    }

    public InvoiceUpdater incDate(Number value) {
      updateOperations.inc("date", value);
      return this;
    }
    public InvoiceUpdater id(org.bson.types.ObjectId value) {
      updateOperations.set("id", value);
      return this;
    }

    public InvoiceUpdater unsetId(org.bson.types.ObjectId value) {
      updateOperations.unset("id");
      return this;
    }

    public InvoiceUpdater addId(org.bson.types.ObjectId value) {
      updateOperations.add("id", value);
      return this;
    }

    public InvoiceUpdater addId(org.bson.types.ObjectId value, boolean addDups) {
      updateOperations.add("id", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToId(java.util.List<org.bson.types.ObjectId> values, boolean addDups) {
      updateOperations.addAll("id", values, addDups);
      return this;
    }
  
    public InvoiceUpdater removeFirstId() {
      updateOperations.removeFirst("id");
      return this;
    }
  
    public InvoiceUpdater removeLastId() {
      updateOperations.removeLast("id");
      return this;
    }
  
    public InvoiceUpdater removeFromId(org.bson.types.ObjectId value) {
      updateOperations.removeAll("id", value);
      return this;
    }

    public InvoiceUpdater removeAllFromId(java.util.List<org.bson.types.ObjectId> values) {
      updateOperations.removeAll("id", values);
      return this;
    }
 
    public InvoiceUpdater decId() {
      updateOperations.dec("id");
      return this;
    }

    public InvoiceUpdater incId() {
      updateOperations.inc("id");
      return this;
    }

    public InvoiceUpdater incId(Number value) {
      updateOperations.inc("id", value);
      return this;
    }
    public InvoiceUpdater items(java.util.List<com.antwerkz.critter.Item> value) {
      updateOperations.set("items", value);
      return this;
    }

    public InvoiceUpdater unsetItems(java.util.List<com.antwerkz.critter.Item> value) {
      updateOperations.unset("items");
      return this;
    }

    public InvoiceUpdater addItems(java.util.List<com.antwerkz.critter.Item> value) {
      updateOperations.add("items", value);
      return this;
    }

    public InvoiceUpdater addItems(java.util.List<com.antwerkz.critter.Item> value, boolean addDups) {
      updateOperations.add("items", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToItems(java.util.List<java.util.List<com.antwerkz.critter.Item>> values, boolean addDups) {
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
  
    public InvoiceUpdater removeFromItems(java.util.List<com.antwerkz.critter.Item> value) {
      updateOperations.removeAll("items", value);
      return this;
    }

    public InvoiceUpdater removeAllFromItems(java.util.List<java.util.List<com.antwerkz.critter.Item>> values) {
      updateOperations.removeAll("items", values);
      return this;
    }
 
    public InvoiceUpdater decItems() {
      updateOperations.dec("items");
      return this;
    }

    public InvoiceUpdater incItems() {
      updateOperations.inc("items");
      return this;
    }

    public InvoiceUpdater incItems(Number value) {
      updateOperations.inc("items", value);
      return this;
    }
    public InvoiceUpdater total(java.lang.Double value) {
      updateOperations.set("total", value);
      return this;
    }

    public InvoiceUpdater unsetTotal(java.lang.Double value) {
      updateOperations.unset("total");
      return this;
    }

    public InvoiceUpdater addTotal(java.lang.Double value) {
      updateOperations.add("total", value);
      return this;
    }

    public InvoiceUpdater addTotal(java.lang.Double value, boolean addDups) {
      updateOperations.add("total", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToTotal(java.util.List<java.lang.Double> values, boolean addDups) {
      updateOperations.addAll("total", values, addDups);
      return this;
    }
  
    public InvoiceUpdater removeFirstTotal() {
      updateOperations.removeFirst("total");
      return this;
    }
  
    public InvoiceUpdater removeLastTotal() {
      updateOperations.removeLast("total");
      return this;
    }
  
    public InvoiceUpdater removeFromTotal(java.lang.Double value) {
      updateOperations.removeAll("total", value);
      return this;
    }

    public InvoiceUpdater removeAllFromTotal(java.util.List<java.lang.Double> values) {
      updateOperations.removeAll("total", values);
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
  }
}
