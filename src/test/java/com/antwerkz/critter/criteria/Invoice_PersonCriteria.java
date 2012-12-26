package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import org.bson.types.ObjectId;

public class Invoice_PersonCriteria {
  private Query<com.antwerkz.critter.Invoice.Person> query;
  private String prefix;

  public Invoice_PersonCriteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix;
  }


  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Person, java.lang.String> first() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".first"));
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice.Person, java.lang.String> last() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".last"));
  }
}
