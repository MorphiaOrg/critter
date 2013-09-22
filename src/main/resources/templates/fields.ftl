<#list fields as field>

  public TypeSafeFieldEnd<${name}Criteria, ${fqcn}, ${field.type}> ${field.name}() {
    return new TypeSafeFieldEnd<>(this, query, prefix + "${field.name}");
  }

  public ${name}Criteria ${field.name}(${field.type} value) {
    new TypeSafeFieldEnd<>(this, query, prefix + "${field.name}").equal(value);
    return this;
  }
</#list>