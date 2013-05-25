package com.antwerkz.critter.criteria;

import java.util.List;

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

  public PersonUpdater getUpdater() {
    return new PersonUpdater();
  }

  public class PersonUpdater {
    UpdateOperations<com.antwerkz.critter.Person> updateOperations;

    public PersonUpdater() {
      updateOperations = ds.createUpdateOperations(com.antwerkz.critter.Person.class);
    }

    public UpdateResults<com.antwerkz.critter.Person> update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults<com.antwerkz.critter.Person> update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults<com.antwerkz.critter.Person> upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults<com.antwerkz.critter.Person> upsert(WriteConcern wc) {
      return ds.update(query(), updateOperations, true, wc);
    }

    public PersonUpdater age(java.lang.Long value) {
      updateOperations.set("age", value);
      return this;
    }

    public PersonUpdater unsetAge(java.lang.Long value) {
      updateOperations.unset("age");
      return this;
    }

    public PersonUpdater addAge(java.lang.Long value) {
      updateOperations.add("age", value);
      return this;
    }

    public PersonUpdater addAge(String fieldExpr, java.lang.Long value, boolean addDups) {
      updateOperations.add("age", value, addDups);
      return this;
    }

    public PersonUpdater addAllToAge(List<java.lang.Long> values, boolean addDups) {
      updateOperations.addAll("age", values, addDups);
      return this;
    }
  
    public PersonUpdater removeFirstAge() {
      updateOperations.removeFirst("age");
      return this;
    }
  
    public PersonUpdater removeLastAge() {
      updateOperations.removeLast("age");
      return this;
    }
  
    public PersonUpdater removeFromAge(java.lang.Long value) {
      updateOperations.removeAll("age", value);
      return this;
    }

    public PersonUpdater removeAllFromAge(List<java.lang.Long> values) {
      updateOperations.removeAll("age", values);
      return this;
    }
 
    public PersonUpdater decAge() {
      updateOperations.dec("age");
      return this;
    }

    public PersonUpdater incAge() {
      updateOperations.inc("age");
      return this;
    }

    public PersonUpdater incAge(Number value) {
      updateOperations.inc("age", value);
      return this;
    }
    public PersonUpdater first(java.lang.String value) {
      updateOperations.set("first", value);
      return this;
    }

    public PersonUpdater unsetFirst(java.lang.String value) {
      updateOperations.unset("first");
      return this;
    }

    public PersonUpdater addFirst(java.lang.String value) {
      updateOperations.add("first", value);
      return this;
    }

    public PersonUpdater addFirst(String fieldExpr, java.lang.String value, boolean addDups) {
      updateOperations.add("first", value, addDups);
      return this;
    }

    public PersonUpdater addAllToFirst(List<java.lang.String> values, boolean addDups) {
      updateOperations.addAll("first", values, addDups);
      return this;
    }
  
    public PersonUpdater removeFirstFirst() {
      updateOperations.removeFirst("first");
      return this;
    }
  
    public PersonUpdater removeLastFirst() {
      updateOperations.removeLast("first");
      return this;
    }
  
    public PersonUpdater removeFromFirst(java.lang.String value) {
      updateOperations.removeAll("first", value);
      return this;
    }

    public PersonUpdater removeAllFromFirst(List<java.lang.String> values) {
      updateOperations.removeAll("first", values);
      return this;
    }
 
    public PersonUpdater decFirst() {
      updateOperations.dec("first");
      return this;
    }

    public PersonUpdater incFirst() {
      updateOperations.inc("first");
      return this;
    }

    public PersonUpdater incFirst(Number value) {
      updateOperations.inc("first", value);
      return this;
    }
    public PersonUpdater id(org.bson.types.ObjectId value) {
      updateOperations.set("id", value);
      return this;
    }

    public PersonUpdater unsetId(org.bson.types.ObjectId value) {
      updateOperations.unset("id");
      return this;
    }

    public PersonUpdater addId(org.bson.types.ObjectId value) {
      updateOperations.add("id", value);
      return this;
    }

    public PersonUpdater addId(String fieldExpr, org.bson.types.ObjectId value, boolean addDups) {
      updateOperations.add("id", value, addDups);
      return this;
    }

    public PersonUpdater addAllToId(List<org.bson.types.ObjectId> values, boolean addDups) {
      updateOperations.addAll("id", values, addDups);
      return this;
    }
  
    public PersonUpdater removeFirstId() {
      updateOperations.removeFirst("id");
      return this;
    }
  
    public PersonUpdater removeLastId() {
      updateOperations.removeLast("id");
      return this;
    }
  
    public PersonUpdater removeFromId(org.bson.types.ObjectId value) {
      updateOperations.removeAll("id", value);
      return this;
    }

    public PersonUpdater removeAllFromId(List<org.bson.types.ObjectId> values) {
      updateOperations.removeAll("id", values);
      return this;
    }
 
    public PersonUpdater decId() {
      updateOperations.dec("id");
      return this;
    }

    public PersonUpdater incId() {
      updateOperations.inc("id");
      return this;
    }

    public PersonUpdater incId(Number value) {
      updateOperations.inc("id", value);
      return this;
    }
    public PersonUpdater last(java.lang.String value) {
      updateOperations.set("last", value);
      return this;
    }

    public PersonUpdater unsetLast(java.lang.String value) {
      updateOperations.unset("last");
      return this;
    }

    public PersonUpdater addLast(java.lang.String value) {
      updateOperations.add("last", value);
      return this;
    }

    public PersonUpdater addLast(String fieldExpr, java.lang.String value, boolean addDups) {
      updateOperations.add("last", value, addDups);
      return this;
    }

    public PersonUpdater addAllToLast(List<java.lang.String> values, boolean addDups) {
      updateOperations.addAll("last", values, addDups);
      return this;
    }
  
    public PersonUpdater removeFirstLast() {
      updateOperations.removeFirst("last");
      return this;
    }
  
    public PersonUpdater removeLastLast() {
      updateOperations.removeLast("last");
      return this;
    }
  
    public PersonUpdater removeFromLast(java.lang.String value) {
      updateOperations.removeAll("last", value);
      return this;
    }

    public PersonUpdater removeAllFromLast(List<java.lang.String> values) {
      updateOperations.removeAll("last", values);
      return this;
    }
 
    public PersonUpdater decLast() {
      updateOperations.dec("last");
      return this;
    }

    public PersonUpdater incLast() {
      updateOperations.inc("last");
      return this;
    }

    public PersonUpdater incLast(Number value) {
      updateOperations.inc("last", value);
      return this;
    }
  }
}
