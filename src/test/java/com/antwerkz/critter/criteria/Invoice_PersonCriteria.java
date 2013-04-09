package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.QueryImpl;

public class Invoice_PersonCriteria {
  private Query<com.antwerkz.critter.Invoice.Person> query;
  private Datastore ds;

  public Query<com.antwerkz.critter.Invoice.Person> query() {
    return query;
  }

  public Invoice_PersonCriteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(com.antwerkz.critter.Invoice.Person.class);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Person, java.lang.String> first() {
    return new TypeSafeFieldEnd<>(query, query.criteria("first"));
  }

  public Invoice_PersonCriteria first(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria("first")).equal(value);
    return this;
  }

  public Invoice_PersonCriteria distinctFirst() {
    ((QueryImpl) query).getCollection().distinct("first");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Person, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(query, query.criteria("id"));
  }

  public Invoice_PersonCriteria id(org.bson.types.ObjectId value) {
    new TypeSafeFieldEnd<>(query, query.criteria("id")).equal(value);
    return this;
  }

  public Invoice_PersonCriteria distinctId() {
    ((QueryImpl) query).getCollection().distinct("id");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Person, java.lang.String> last() {
    return new TypeSafeFieldEnd<>(query, query.criteria("last"));
  }

  public Invoice_PersonCriteria last(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria("last")).equal(value);
    return this;
  }

  public Invoice_PersonCriteria distinctLast() {
    ((QueryImpl) query).getCollection().distinct("last");
    return this;
  }
}
