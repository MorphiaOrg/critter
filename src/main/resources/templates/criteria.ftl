package ${package}.criteria;

import com.antwerkz.critter.TypeSafeFieldEnd;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Criteria;
import com.google.code.morphia.query.CriteriaContainer;
import com.google.code.morphia.query.Query;
import org.bson.types.ObjectId;
<#--
<#list fields as field>
import ${field.type};
</#list>
-->

public class ${name}Criteria {
  private Query<${fqcn}> query;

  public Query<${fqcn}> query() {
    return query;
  }

  public ${name}Criteria(Datastore ds) {
    query = ds.find(${fqcn}.class);
  }

  public CriteriaContainer or(Criteria... criteria) {
    return query.or(criteria);
  }

  public CriteriaContainer and(Criteria... criteria) {
    return query.and(criteria);
  }
<#list fields as field>

  public TypeSafeFieldEnd<? extends CriteriaContainer, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<>(query.criteria("${field.name}"));
  }
</#list>
}
