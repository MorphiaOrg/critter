package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Invoice;
import com.antwerkz.critter.Item;
import com.antwerkz.critter.Person;
import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import org.bson.types.ObjectId;

import java.lang.Double;
import java.util.Date;
import java.util.List;


public class InvoiceCriteria {
  private Query<Invoice> query;
  private Datastore ds;

  public Query<Invoice> query() {
    return query;
  }

  public InvoiceCriteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(Invoice.class);
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

  public TypeSafeFieldEnd<? extends CriteriaContainer, Invoice, Date> date() {
    return new TypeSafeFieldEnd<>(query, query.criteria("date"));
  }

  public InvoiceCriteria date(Date value) {
    new TypeSafeFieldEnd<>(query, query.criteria("date")).equal(value);
    return this;
  }

  public InvoiceCriteria orderByDate() {
    return orderByDate(true);
  }

  public InvoiceCriteria orderByDate(boolean ascending) {
    query.order((!ascending ? "-" : "") + "date");
    return this;
  }

  public InvoiceCriteria distinctDate() {
    ((QueryImpl) query).getCollection().distinct("date");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, Invoice, ObjectId> id() {
    return new TypeSafeFieldEnd<>(query, query.criteria("id"));
  }

  public InvoiceCriteria id(ObjectId value) {
    new TypeSafeFieldEnd<>(query, query.criteria("id")).equal(value);
    return this;
  }

  public InvoiceCriteria orderById() {
    return orderById(true);
  }

  public InvoiceCriteria orderById(boolean ascending) {
    query.order((!ascending ? "-" : "") + "id");
    return this;
  }

  public InvoiceCriteria distinctId() {
    ((QueryImpl) query).getCollection().distinct("id");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, Invoice, List<Item>> items() {
    return new TypeSafeFieldEnd<>(query, query.criteria("items"));
  }

  public InvoiceCriteria items(List<Item> value) {
    new TypeSafeFieldEnd<>(query, query.criteria("items")).equal(value);
    return this;
  }

  public InvoiceCriteria orderByItems() {
    return orderByItems(true);
  }

  public InvoiceCriteria orderByItems(boolean ascending) {
    query.order((!ascending ? "-" : "") + "items");
    return this;
  }

  public InvoiceCriteria distinctItems() {
    ((QueryImpl) query).getCollection().distinct("items");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, Invoice, Double> total() {
    return new TypeSafeFieldEnd<>(query, query.criteria("total"));
  }

  public InvoiceCriteria total(Double value) {
    new TypeSafeFieldEnd<>(query, query.criteria("total")).equal(value);
    return this;
  }

  public InvoiceCriteria orderByTotal() {
    return orderByTotal(true);
  }

  public InvoiceCriteria orderByTotal(boolean ascending) {
    query.order((!ascending ? "-" : "") + "total");
    return this;
  }

  public InvoiceCriteria distinctTotal() {
    ((QueryImpl) query).getCollection().distinct("total");
    return this;
  }

  public Invoice_AddressCriteria addresses() {
    return new Invoice_AddressCriteria(query, "addresses");
  }

  public InvoiceCriteria person(Person reference) {
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

    public UpdateResults<Invoice> update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults<Invoice> update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults<Invoice> upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults<Invoice> upsert(WriteConcern wc) {
      return ds.update(query(), updateOperations, true, wc);
    }

    public InvoiceUpdater date(Date value) {
      updateOperations.set("date", value);
      return this;
    }

    public InvoiceUpdater unsetDate(Date value) {
      updateOperations.unset("date");
      return this;
    }

    public InvoiceUpdater addDate(Date value) {
      updateOperations.add("date", value);
      return this;
    }

    public InvoiceUpdater addDate(String fieldExpr, Date value, boolean addDups) {
      updateOperations.add("date", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToDate(List<Date> values, boolean addDups) {
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
  
    public InvoiceUpdater removeFromDate(Date value) {
      updateOperations.removeAll("date", value);
      return this;
    }

    public InvoiceUpdater removeAllFromDate(List<Date> values) {
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
    public InvoiceUpdater id(ObjectId value) {
      updateOperations.set("id", value);
      return this;
    }

    public InvoiceUpdater unsetId(ObjectId value) {
      updateOperations.unset("id");
      return this;
    }

    public InvoiceUpdater addId(ObjectId value) {
      updateOperations.add("id", value);
      return this;
    }

    public InvoiceUpdater addId(String fieldExpr, ObjectId value, boolean addDups) {
      updateOperations.add("id", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToId(List<ObjectId> values, boolean addDups) {
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
  
    public InvoiceUpdater removeFromId(ObjectId value) {
      updateOperations.removeAll("id", value);
      return this;
    }

    public InvoiceUpdater removeAllFromId(List<ObjectId> values) {
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
    public InvoiceUpdater items(List<Item> value) {
      updateOperations.set("items", value);
      return this;
    }

    public InvoiceUpdater unsetItems(List<Item> value) {
      updateOperations.unset("items");
      return this;
    }

    public InvoiceUpdater addItems(List<Item> value) {
      updateOperations.add("items", value);
      return this;
    }

    public InvoiceUpdater addItems(String fieldExpr, List<Item> value, boolean addDups) {
      updateOperations.add("items", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToItems(List<List<Item>> values, boolean addDups) {
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
  
    public InvoiceUpdater removeFromItems(List<Item> value) {
      updateOperations.removeAll("items", value);
      return this;
    }

    public InvoiceUpdater removeAllFromItems(List<List<Item>> values) {
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
    public InvoiceUpdater total(Double value) {
      updateOperations.set("total", value);
      return this;
    }

    public InvoiceUpdater unsetTotal(Double value) {
      updateOperations.unset("total");
      return this;
    }

    public InvoiceUpdater addTotal(Double value) {
      updateOperations.add("total", value);
      return this;
    }

    public InvoiceUpdater addTotal(String fieldExpr, Double value, boolean addDups) {
      updateOperations.add("total", value, addDups);
      return this;
    }

    public InvoiceUpdater addAllToTotal(List<Double> values, boolean addDups) {
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
  
    public InvoiceUpdater removeFromTotal(Double value) {
      updateOperations.removeAll("total", value);
      return this;
    }

    public InvoiceUpdater removeAllFromTotal(List<Double> values) {
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
