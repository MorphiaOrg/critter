package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import java.util.List;


public class InvoiceCriteria {
  private Query<com.antwerkz.critter.Invoice> query;
  private Datastore ds;
  private String prefix = "";

  public Query<com.antwerkz.critter.Invoice> query() {
    return query;
  }

  public InvoiceCriteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(com.antwerkz.critter.Invoice.class);
  }

  public WriteResult delete() {
     return ds.delete(query());
  }

  public WriteResult delete(WriteConcern wc) {
     return ds.delete(query(), wc);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }


  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.util.Date> date() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "date"));
  }

  public InvoiceCriteria date(java.util.Date value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "date")).equal(value);
    return this;
  }

  public InvoiceCriteria orderByDate() {
    return orderByDate(true);
  }

  public InvoiceCriteria orderByDate(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "date");
    return this;
  }

  public InvoiceCriteria distinctDate() {
    ((QueryImpl) query).getCollection().distinct(prefix + "date");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "id"));
  }

  public InvoiceCriteria id(org.bson.types.ObjectId value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "id")).equal(value);
    return this;
  }

  public InvoiceCriteria orderById() {
    return orderById(true);
  }

  public InvoiceCriteria orderById(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "id");
    return this;
  }

  public InvoiceCriteria distinctId() {
    ((QueryImpl) query).getCollection().distinct(prefix + "id");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.util.List<com.antwerkz.critter.Item>> items() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "items"));
  }

  public InvoiceCriteria items(java.util.List<com.antwerkz.critter.Item> value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "items")).equal(value);
    return this;
  }

  public InvoiceCriteria orderByItems() {
    return orderByItems(true);
  }

  public InvoiceCriteria orderByItems(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "items");
    return this;
  }

  public InvoiceCriteria distinctItems() {
    ((QueryImpl) query).getCollection().distinct(prefix + "items");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.lang.Double> total() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "total"));
  }

  public InvoiceCriteria total(java.lang.Double value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "total")).equal(value);
    return this;
  }

  public InvoiceCriteria orderByTotal() {
    return orderByTotal(true);
  }

  public InvoiceCriteria orderByTotal(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "total");
    return this;
  }

  public InvoiceCriteria distinctTotal() {
    ((QueryImpl) query).getCollection().distinct(prefix + "total");
    return this;
  }


  public com.antwerkz.critter.criteria.Invoice_AddressCriteria addresses() {
    return new com.antwerkz.critter.criteria.Invoice_AddressCriteria(query, "addresses");
  }

  public InvoiceCriteria person(com.antwerkz.critter.Person reference) {
    query.filter("person = ", reference);
    return this;
  }

  public InvoiceUpdater getUpdater() {
    return new InvoiceUpdater();
  }

  public class InvoiceUpdater {
    UpdateOperations<com.antwerkz.critter.Invoice> updateOperations;

    public InvoiceUpdater() {
      updateOperations = ds.createUpdateOperations(com.antwerkz.critter.Invoice.class);
    }

    public UpdateResults<com.antwerkz.critter.Invoice> update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults<com.antwerkz.critter.Invoice> update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults<com.antwerkz.critter.Invoice> upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults<com.antwerkz.critter.Invoice> upsert(WriteConcern wc) {
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

    public InvoiceUpdater addDate(String fieldExpr, java.util.Date value, boolean addDups) {
      updateOperations.add("date", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToDate(List<java.util.Date> values, boolean addDups) {
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

    public InvoiceUpdater removeAllFromDate(List<java.util.Date> values) {
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

    public InvoiceUpdater addId(String fieldExpr, org.bson.types.ObjectId value, boolean addDups) {
      updateOperations.add("id", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToId(List<org.bson.types.ObjectId> values, boolean addDups) {
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

    public InvoiceUpdater removeAllFromId(List<org.bson.types.ObjectId> values) {
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

    public InvoiceUpdater addItems(String fieldExpr, java.util.List<com.antwerkz.critter.Item> value, boolean addDups) {
      updateOperations.add("items", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToItems(List<java.util.List<com.antwerkz.critter.Item>> values, boolean addDups) {
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

    public InvoiceUpdater removeAllFromItems(List<java.util.List<com.antwerkz.critter.Item>> values) {
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

    public InvoiceUpdater addTotal(String fieldExpr, java.lang.Double value, boolean addDups) {
      updateOperations.add("total", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToTotal(List<java.lang.Double> values, boolean addDups) {
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

    public InvoiceUpdater removeAllFromTotal(List<java.lang.Double> values) {
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
