package com.antwerkz.critter.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import org.bson.types.ObjectId;

public class InvoiceCriteria {
  private Query<com.antwerkz.critter.Invoice> query;

  public Query<com.antwerkz.critter.Invoice> query() {
    return query;
  }

  public InvoiceCriteria(Datastore ds) {
    query = ds.find(com.antwerkz.critter.Invoice.class);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.util.Date> date() {
    return new TypeSafeFieldEnd<>(query, query.criteria("date"));
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, org.bson.types.ObjectId> id() {
    return new TypeSafeFieldEnd<>(query, query.criteria("id"));
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.util.List<com.antwerkz.critter.Item>> items() {
    return new TypeSafeFieldEnd<>(query, query.criteria("items"));
  }

  public TypeSafeFieldEnd<? extends CriteriaContainer, com.antwerkz.critter.Invoice, java.lang.Double> total() {
    return new TypeSafeFieldEnd<>(query, query.criteria("total"));
  }

  public com.antwerkz.critter.criteria.Invoice_PersonCriteria person() {
    return new com.antwerkz.critter.criteria.Invoice_PersonCriteria(query, "person");
  }
}
