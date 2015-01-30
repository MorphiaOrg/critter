package com.antwerkz.critter.criteria;

import com.antwerkz.critter.criteria.BaseCriteria;
import com.antwerkz.critter.test.Person;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.FieldEndImpl;
import org.mongodb.morphia.query.QueryImpl;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import com.mongodb.WriteConcern;
import java.lang.Long;
import java.lang.String;

public class PersonCriteria extends BaseCriteria<Person>
{

   public PersonCriteria(Datastore ds)
   {
      super(ds, Person.class);
   }

   public TypeSafeFieldEnd<PersonCriteria, Person, Long> age()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.PersonCriteria, com.antwerkz.critter.test.Person, java.lang.Long>(
            this, query, "age");
   }

   public Criteria age(Long value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "age",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<PersonCriteria, Person, String> first()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.PersonCriteria, com.antwerkz.critter.test.Person, java.lang.String>(
            this, query, "first");
   }

   public Criteria first(String value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "first",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<PersonCriteria, Person, ObjectId> id()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.PersonCriteria, com.antwerkz.critter.test.Person, org.bson.types.ObjectId>(
            this, query, "id");
   }

   public Criteria id(ObjectId value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "id",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<PersonCriteria, Person, String> last()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.PersonCriteria, com.antwerkz.critter.test.Person, java.lang.String>(
            this, query, "last");
   }

   public Criteria last(String value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "last",
            (QueryImpl) query, false).equal(value);
   }

   public PersonUpdater getUpdater()
   {
      return new PersonUpdater();
   }

   public class PersonUpdater
   {
      UpdateOperations<Person> updateOperations;

      public PersonUpdater()
      {
         updateOperations = ds.createUpdateOperations(Person.class);
      }

      public UpdateResults update()
      {
         return ds.update(query(), updateOperations, false);
      }

      public UpdateResults update(WriteConcern wc)
      {
         return ds.update(query(), updateOperations, false, wc);
      }

      public UpdateResults upsert()
      {
         return ds.update(query(), updateOperations, true);
      }

      public UpdateResults upsert(WriteConcern wc)
      {
         return ds.update(query(), updateOperations, true, wc);
      }

      public PersonUpdater age(Long value)
      {
         updateOperations.set("age", value);
         return this;
      }

      public PersonUpdater unsetAge()
      {
         updateOperations.unset("age");
         return this;
      }

      public PersonUpdater decAge()
      {
         updateOperations.dec("age");
         return this;
      }

      public PersonUpdater incAge()
      {
         updateOperations.inc("age");
         return this;
      }

      public PersonUpdater incAge(Long value)
      {
         updateOperations.inc("age", value);
         return this;
      }

      public PersonUpdater first(String value)
      {
         updateOperations.set("first", value);
         return this;
      }

      public PersonUpdater unsetFirst()
      {
         updateOperations.unset("first");
         return this;
      }

      public PersonUpdater last(String value)
      {
         updateOperations.set("last", value);
         return this;
      }

      public PersonUpdater unsetLast()
      {
         updateOperations.unset("last");
         return this;
      }
   }
}
