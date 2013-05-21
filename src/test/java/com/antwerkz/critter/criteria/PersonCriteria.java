package com.antwerkz.critter.criteria;

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


public class PersonCriteria {
  private Query<com.antwerkz.critter.Person> query;
  private Datastore ds;

  public Query<com.antwerkz.critter.Person> query() {
    return query;
  }

  public PersonCriteria(Datastore ds) {
    this.ds = ds;
    query = ds.find(com.antwerkz.critter.Person.class);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Person, java.lang.Long> age() {
    return new TypeSafeFieldEnd<>(query, query.criteria("age"));
  }

  public PersonCriteria age(java.lang.Long value) {
    new TypeSafeFieldEnd<>(query, query.criteria("age")).equal(value);
    return this;
  }

  public PersonCriteria orderByAge() {
    return orderByAge(true);
  }

  public PersonCriteria orderByAge(boolean ascending) {
    query.order((!ascending ? "-" : "") + "age");
    return this;
  }

  public PersonCriteria distinctAge() {
    ((QueryImpl) query).getCollection().distinct("age");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Person, java.lang.String> first() {
    return new TypeSafeFieldEnd<>(query, query.criteria("first"));
  }

  public PersonCriteria first(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria("first")).equal(value);
    return this;
  }

  public PersonCriteria orderByFirst() {
    return orderByFirst(true);
  }

  public PersonCriteria orderByFirst(boolean ascending) {
    query.order((!ascending ? "-" : "") + "first");
    return this;
  }

  public PersonCriteria distinctFirst() {
    ((QueryImpl) query).getCollection().distinct("first");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Person, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(query, query.criteria("id"));
  }

  public PersonCriteria id(org.bson.types.ObjectId value) {
    new TypeSafeFieldEnd<>(query, query.criteria("id")).equal(value);
    return this;
  }

  public PersonCriteria orderById() {
    return orderById(true);
  }

  public PersonCriteria orderById(boolean ascending) {
    query.order((!ascending ? "-" : "") + "id");
    return this;
  }

  public PersonCriteria distinctId() {
    ((QueryImpl) query).getCollection().distinct("id");
    return this;
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Person, java.lang.String> last() {
    return new TypeSafeFieldEnd<>(query, query.criteria("last"));
  }

  public PersonCriteria last(java.lang.String value) {
    new TypeSafeFieldEnd<>(query, query.criteria("last")).equal(value);
    return this;
  }

  public PersonCriteria orderByLast() {
    return orderByLast(true);
  }

  public PersonCriteria orderByLast(boolean ascending) {
    query.order((!ascending ? "-" : "") + "last");
    return this;
  }

  public PersonCriteria distinctLast() {
    ((QueryImpl) query).getCollection().distinct("last");
    return this;
  }

  public PersonUpdater update() {
    return new PersonUpdater();
  }

  public class PersonUpdater {
    UpdateOperations<com.antwerkz.critter.Person> updateOperations;

    public PersonUpdater() {
      updateOperations = ds.createUpdateOperations(com.antwerkz.critter.Person.class);
    }

    public UpdateResults<Person> update() {
      return ds.update(query(), updateOperations, false);
    }

    public void update(WriteConcern wc) {
      ds.update(query(), updateOperations, false, wc);
    }

    public void upsert() {
      ds.update(query(), updateOperations, true);
    }

    public void upsert(WriteConcern wc) {
      ds.update(query(), updateOperations, true, wc);
    }

    public PersonUpdater age(java.lang.Long value) {
      updateOperations.set("age", value);
      return this;
    }
    public PersonUpdater first(java.lang.String value) {
      updateOperations.set("first", value);
      return this;
    }
    public PersonUpdater id(org.bson.types.ObjectId value) {
      updateOperations.set("id", value);
      return this;
    }
    public PersonUpdater last(java.lang.String value) {
      updateOperations.set("last", value);
      return this;
    }
  }
}
