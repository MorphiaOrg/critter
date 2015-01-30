package com.antwerkz.critter.criteria;

import org.mongodb.morphia.query.Query;
import com.antwerkz.critter.test.Item;
import org.mongodb.morphia.query.Criteria;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.FieldEndImpl;
import org.mongodb.morphia.query.QueryImpl;

public class ItemCriteria
{

   private Query query;
   private String prefix;

   public ItemCriteria(Query query, String prefix)
   {
      this.query = query;
      this.prefix = prefix + ".";
   }

   public TypeSafeFieldEnd<ItemCriteria, Item, String> name()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.ItemCriteria, com.antwerkz.critter.test.Item, java.lang.String>(
            this, query, prefix + "name");
   }

   public Criteria name(String value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, prefix + "name",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<ItemCriteria, Item, Double> price()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.ItemCriteria, com.antwerkz.critter.test.Item, java.lang.Double>(
            this, query, prefix + "price");
   }

   public Criteria price(Double value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, prefix + "price",
            (QueryImpl) query, false).equal(value);
   }
}
