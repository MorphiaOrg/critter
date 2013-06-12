<#list fields as field>

  public TypeSafeFieldEnd<? extends CriteriaContainer, ${fqcn}, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<>(query, query.criteria(prefix + "${field.name}"));
  }

  public ${name}Criteria ${field.name}(${field.type} value) {
    new TypeSafeFieldEnd<>(query, query.criteria(prefix + "${field.name}")).equal(value);
    return this;
  }

  public ${name}Criteria orderBy${field.name?cap_first}() {
    return orderBy${field.name?cap_first}(true);
  }

  public ${name}Criteria orderBy${field.name?cap_first}(boolean ascending) {
    query.order((!ascending ? "-" : "") + prefix + "${field.name}");
    return this;
  }

  public ${name}Criteria distinct${field.name?cap_first}() {
    ((QueryImpl) query).getCollection().distinct(prefix + "${field.name}");
    return this;
  }
</#list>