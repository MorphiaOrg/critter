package com.antwerkz.critter.test.criteria;

import com.antwerkz.critter.test.Person;
import com.mongodb.WriteConcern;
import com.antwerkz.critter.criteria.BaseCriteria;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import java.util.List;

public class PersonCriteria extends BaseCriteria<Person> {
  private String prefix = "";

  public PersonCriteria(Datastore ds) {
    super(ds, Person.class);
  }


  // fields
  public TypeSafeFieldEnd<PersonCriteria, Person, java.lang.Long> age() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "age");
  }

  public Criteria age(java.lang.Long value) {
    return new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.Long>(this, query, prefix + "age").equal(value);
  }
  // end fields

  // fields
  public TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String> first() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "first");
  }

  public Criteria first(java.lang.String value) {
    return new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String>(this, query, prefix + "first").equal(value);
  }
  // end fields

  // fields
  public TypeSafeFieldEnd<PersonCriteria, Person, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "id");
  }

  public Criteria id(org.bson.types.ObjectId value) {
    return new TypeSafeFieldEnd<PersonCriteria, Person, org.bson.types.ObjectId>(this, query, prefix + "id").equal(value);
  }
  // end fields

  // fields
  public TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String> last() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "last");
  }

  public Criteria last(java.lang.String value) {
    return new TypeSafeFieldEnd<PersonCriteria, Person, java.lang.String>(this, query, prefix + "last").equal(value);
  }
  // end fields


  public PersonUpdater getUpdater() {
    return new PersonUpdater();
  }

  public class PersonUpdater {
    UpdateOperations<Person> updateOperations;

    public PersonUpdater() {
      updateOperations = ds.createUpdateOperations(Person.class);
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
    public PersonUpdater age(java.lang.Long value) {
    updateOperations.set("age", value);
    return this;
    }

    public PersonUpdater unsetAge() {
    updateOperations.unset("age");
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

    public PersonUpdater unsetFirst() {
    updateOperations.unset("first");
    return this;
    }


    public PersonUpdater last(java.lang.String value) {
    updateOperations.set("last", value);
    return this;
    }

    public PersonUpdater unsetLast() {
    updateOperations.unset("last");
    return this;
    }


    // Updater Methods
  }
}
