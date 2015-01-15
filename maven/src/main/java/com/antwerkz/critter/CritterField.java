package com.antwerkz.critter;

import java.util.List;

import static java.lang.String.format;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;

public class CritterField implements Comparable<CritterField> {
  private Field<JavaClassSource> source;

  private String fullType;

  private JavaClassSource javaClass;

  public CritterField(final CritterContext context, final JavaClassSource javaClass,
      final Field<JavaClassSource> field) {
    this.javaClass = javaClass;
    source = field;
    fullType = field.getType().getQualifiedName();
    if (field.getType().isParameterized()) {
      final List<Type<JavaClassSource>> typeArguments = field.getType().getTypeArguments();
      System.out.println("typeArguments = " + typeArguments);
    }
  }

  public void build(final JavaClassSource criteriaClass) {
    criteriaClass.addMethod()
        .setName(source.getName())
        .setPublic()
        .setReturnType(format("%s<%s, %s, %s>", TypeSafeFieldEnd.class.getName(), criteriaClass.getQualifiedName(),
            javaClass.getQualifiedName(), fullType))
        .setBody(format("return new TypeSafeFieldEnd<>(this, query, prefix + \"%s\");", source.getName()));

    final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
        .setName(source.getName())
        .setPublic()
        .setReturnType("Criteria")
        .setBody(format("return new TypeSafeFieldEnd<%s, %s, %s>(this, query, prefix + \"%s\").equal(value);",
            criteriaClass.getName(), javaClass.getName(), source.getType().getName(), source.getName()));

    method.addParameter(source.getType().getQualifiedName(), "value");
  }

  @Override
  public int compareTo(final CritterField other) {
    return source.getName().compareTo(other.source.getName());
  }
}
