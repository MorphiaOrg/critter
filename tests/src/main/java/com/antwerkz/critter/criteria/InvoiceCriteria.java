package com.antwerkz.critter.criteria;

import com.antwerkz.critter.criteria.BaseCriteria;
import com.antwerkz.critter.test.Invoice;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Criteria;
import com.antwerkz.critter.criteria.AddressCriteria;
import com.antwerkz.critter.TypeSafeFieldEnd;
import org.mongodb.morphia.query.FieldEndImpl;
import org.mongodb.morphia.query.QueryImpl;
import java.util.Date;
import org.bson.types.ObjectId;
import java.util.List;
import com.antwerkz.critter.test.Person;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import com.mongodb.WriteConcern;
import com.antwerkz.critter.test.Address;
import com.antwerkz.critter.test.Item;
import java.lang.Double;

public class InvoiceCriteria extends BaseCriteria<Invoice>
{

   public InvoiceCriteria(Datastore ds)
   {
      super(ds, Invoice.class);
   }

   public AddressCriteria addresses()
   {
      return new AddressCriteria(query, "addresses");
   }

   public TypeSafeFieldEnd<InvoiceCriteria, Invoice, Date> date()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.InvoiceCriteria, com.antwerkz.critter.test.Invoice, java.util.Date>(
            this, query, "date");
   }

   public Criteria date(Date value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "date",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<InvoiceCriteria, Invoice, ObjectId> id()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.InvoiceCriteria, com.antwerkz.critter.test.Invoice, org.bson.types.ObjectId>(
            this, query, "id");
   }

   public Criteria id(ObjectId value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "id",
            (QueryImpl) query, false).equal(value);
   }

   public TypeSafeFieldEnd<InvoiceCriteria, Invoice, List> items()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.InvoiceCriteria, com.antwerkz.critter.test.Invoice, java.util.List>(
            this, query, "items");
   }

   public Criteria items(List value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "items",
            (QueryImpl) query, false).equal(value);
   }

   public InvoiceCriteria person(Person reference)
   {
      query.filter("person = ", reference);
      return this;
   }

   public TypeSafeFieldEnd<InvoiceCriteria, Invoice, Double> total()
   {
      return new TypeSafeFieldEnd<com.antwerkz.critter.criteria.InvoiceCriteria, com.antwerkz.critter.test.Invoice, java.lang.Double>(
            this, query, "total");
   }

   public Criteria total(Double value)
   {
      return new FieldEndImpl<QueryImpl>((QueryImpl) query, "total",
            (QueryImpl) query, false).equal(value);
   }

   public InvoiceUpdater getUpdater()
   {
      return new InvoiceUpdater();
   }

   public class InvoiceUpdater
   {
      UpdateOperations<Invoice> updateOperations;

      public InvoiceUpdater()
      {
         updateOperations = ds.createUpdateOperations(Invoice.class);
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

      public InvoiceUpdater addresses(List<Address> value)
      {
         updateOperations.set("addresses", value);
         return this;
      }

      public InvoiceUpdater unsetAddresses()
      {
         updateOperations.unset("addresses");
         return this;
      }

      public InvoiceUpdater addToAddresses(Address value)
      {
         updateOperations.add("addresses", value);
         return this;
      }

      public InvoiceUpdater addToAddresses(Address value, boolean addDups)
      {
         updateOperations.add("addresses", value, addDups);
         return this;
      }

      public InvoiceUpdater addAllToAddresses(List<Address> values,
            boolean addDups)
      {
         updateOperations.addAll("addresses", values, addDups);
         return this;
      }

      public InvoiceUpdater removeFirstFromAddresses()
      {
         updateOperations.removeFirst("addresses");
         return this;
      }

      public InvoiceUpdater removeLastFromAddresses()
      {
         updateOperations.removeLast("addresses");
         return this;
      }

      public InvoiceUpdater removeFromAddresses(Address value)
      {
         updateOperations.removeAll("addresses", value);
         return this;
      }

      public InvoiceUpdater removeAllFromAddresses(Address values)
      {
         updateOperations.removeAll("addresses", values);
         return this;
      }

      public InvoiceUpdater date(Date value)
      {
         updateOperations.set("date", value);
         return this;
      }

      public InvoiceUpdater unsetDate()
      {
         updateOperations.unset("date");
         return this;
      }

      public InvoiceUpdater items(List<Item> value)
      {
         updateOperations.set("items", value);
         return this;
      }

      public InvoiceUpdater unsetItems()
      {
         updateOperations.unset("items");
         return this;
      }

      public InvoiceUpdater addToItems(Item value)
      {
         updateOperations.add("items", value);
         return this;
      }

      public InvoiceUpdater addToItems(Item value, boolean addDups)
      {
         updateOperations.add("items", value, addDups);
         return this;
      }

      public InvoiceUpdater addAllToItems(List<Item> values, boolean addDups)
      {
         updateOperations.addAll("items", values, addDups);
         return this;
      }

      public InvoiceUpdater removeFirstFromItems()
      {
         updateOperations.removeFirst("items");
         return this;
      }

      public InvoiceUpdater removeLastFromItems()
      {
         updateOperations.removeLast("items");
         return this;
      }

      public InvoiceUpdater removeFromItems(Item value)
      {
         updateOperations.removeAll("items", value);
         return this;
      }

      public InvoiceUpdater removeAllFromItems(Item values)
      {
         updateOperations.removeAll("items", values);
         return this;
      }

      public InvoiceUpdater person(Person value)
      {
         updateOperations.set("person", value);
         return this;
      }

      public InvoiceUpdater unsetPerson()
      {
         updateOperations.unset("person");
         return this;
      }

      public InvoiceUpdater total(Double value)
      {
         updateOperations.set("total", value);
         return this;
      }

      public InvoiceUpdater unsetTotal()
      {
         updateOperations.unset("total");
         return this;
      }

      public InvoiceUpdater decTotal()
      {
         updateOperations.dec("total");
         return this;
      }

      public InvoiceUpdater incTotal()
      {
         updateOperations.inc("total");
         return this;
      }

      public InvoiceUpdater incTotal(Double value)
      {
         updateOperations.inc("total", value);
         return this;
      }
   }
}
