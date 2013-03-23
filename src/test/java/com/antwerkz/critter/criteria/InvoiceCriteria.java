package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;

public class InvoiceCriteria {
  private Query<com.antwerkz.critter.Invoice> query;
  private Datastore ds;

  public Query<com.antwerkz.critter.Invoice> query() {
    return query;
  }

  public InvoiceCriteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(com.antwerkz.critter.Invoice.class);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.util.Date> date() {
    return new TypeSafeFieldEnd<>(query, query.criteria("date"));
  }

  public InvoiceCriteria distinctDate() {
    ((QueryImpl) query).getCollection().distinct("date");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(query, query.criteria("id"));
  }

  public InvoiceCriteria distinctId() {
    ((QueryImpl) query).getCollection().distinct("id");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.util.List<com.antwerkz.critter.Item>> items() {
    return new TypeSafeFieldEnd<>(query, query.criteria("items"));
  }

  public InvoiceCriteria distinctItems() {
    ((QueryImpl) query).getCollection().distinct("items");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.lang.Double> total() {
    return new TypeSafeFieldEnd<>(query, query.criteria("total"));
  }

  public InvoiceCriteria distinctTotal() {
    ((QueryImpl) query).getCollection().distinct("total");
    return this;
  }

  public com.antwerkz.critter.criteria.Invoice_AddressCriteria address() {
    return new com.antwerkz.critter.criteria.Invoice_AddressCriteria(query, "address");
  }

  public InvoiceCriteria person(com.antwerkz.critter.Invoice.Person reference) {
    query.filter("person = ", reference);
    return this;
  }
}
