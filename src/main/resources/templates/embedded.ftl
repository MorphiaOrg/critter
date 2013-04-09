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
  private String prefix;

  public ${name}Criteria(Query query, String prefix) {
    this.query = query;
    this.prefix = prefix;
  }

<#list fields as field>

  public TypeSafeFieldEnd<? extends CriteriaContainer, ${fqcn}, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".${field.name}"));
  }

  public ${name}Criteria ${field.name}(${field.type} value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + ".${field.name}")).equal(value);
    return this;
  }
</#list>
}
