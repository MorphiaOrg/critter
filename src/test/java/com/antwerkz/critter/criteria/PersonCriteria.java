package com.antwerkz.critter.criteria;

import com.antwerkz.critter.Person;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import java.util.List;

public class PersonCriteria extends BaseCriteria<Person> {
  private String prefix = "";

  public PersonCriteria(Datastore ds) {
    super(ds, Person.class);
  }


  public TypeSafeFieldEnd<PersonCriteria, Person, java.lang.Long> age() {
    return new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.Long>(this, query, prefix + "age");
  }

  public PersonCriteria age(java.lang.Long value) {
    new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.Long>(this, query, prefix + "age").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String> first() {
    return new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String>(this, query, prefix + "first");
  }

  public PersonCriteria first(java.lang.String value) {
    new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String>(this, query, prefix + "first").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<PersonCriteria, Person, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<PersonCriteria, Person, org.bson.types.ObjectId>(this, query, prefix + "id");
  }

  public PersonCriteria id(org.bson.types.ObjectId value) {
    new TypeSafeFieldEnd<PersonCriteria, Person, org.bson.types.ObjectId>(this, query, prefix + "id").equal(value);
    return this;
  }

  public TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String> last() {
    return new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String>(this, query, prefix + "last");
  }

  public PersonCriteria last(java.lang.String value) {
    new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String>(this, query, prefix + "last").equal(value);
    return this;
  }


  public PersonUpdater getUpdater() {
    return new PersonUpdater();
  }

  public class PersonUpdater {
    UpdateOperations<Person> updateOperations;

    public PersonUpdater() {
      updateOperations = ds.createUpdateOperations(Person.class);
    }

    public UpdateResults<Person> update() {
      return ds.update(query(), updateOperations, false);
    }

    public UpdateResults<Person> update(WriteConcern wc) {
      return ds.update(query(), updateOperations, false, wc);
    }

    public UpdateResults<Person> upsert() {
      return ds.update(query(), updateOperations, true);
    }

    public UpdateResults<Person> upsert(WriteConcern wc) {
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

    public PersonUpdater addAge(java.lang.Long value, boolean addDups) {
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

    public PersonUpdater addFirst(java.lang.String value, boolean addDups) {
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

    public PersonUpdater addId(org.bson.types.ObjectId value, boolean addDups) {
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

    public PersonUpdater addLast(java.lang.String value, boolean addDups) {
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
