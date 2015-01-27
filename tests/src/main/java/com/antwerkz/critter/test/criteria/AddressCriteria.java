package com.antwerkz.critter.test.criteria;

import org.mongodb.morphia.query.Query;
import com.antwerkz.critter.test.Address;
import org.mongodb.morphia.query.Criteria;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.FieldEndImpl;
import org.mongodb.morphia.query.QueryImpl;

public class AddressCriteria
{

   private Query query;
   private String prefix;

   public AddressCriteria(Query query, String prefix)
   {
      this.query = query;
      this.prefix = prefix + ".";
   }

   public TypeSafeFieldEnd<AddressCriteria, Address, String> city()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.test.criteria.AddressCriteria, com.antwerkz.critter.test.Address, java.lang.String>(
            this, query, prefix + "city");
   }

   public Criteria city(String value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, prefix + "city",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<AddressCriteria, Address, String> state()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.test.criteria.AddressCriteria, com.antwerkz.critter.test.Address, java.lang.String>(
            this, query, prefix + "state");
   }

   public Criteria state(String value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, prefix + "state",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<AddressCriteria, Address, String> zip()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.test.criteria.AddressCriteria, com.antwerkz.critter.test.Address, java.lang.String>(
            this, query, prefix + "zip");
   }

   public Criteria zip(String value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, prefix + "zip",
            (QueryImpl) query, false).equal(value);
   }
}
