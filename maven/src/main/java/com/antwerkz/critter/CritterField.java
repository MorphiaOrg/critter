package com.antwerkz.critter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Strings;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FieldEndImpl;
import org.mongodb.morphia.query.QueryImpl;

public class CritterField implements Comparable<CritterField> {
  private List<String> shortParameterTypes = new ArrayList<>();

  private List<String> fullParameterTypes = new ArrayList<>();

  private Field<JavaClassSource> source;

  private String fullType;

  private CritterContext context;

  public static final List<String> NUMERIC_TYPES = new ArrayList<>();

  static {
    NUMERIC_TYPES.add("java.lang.Float");
    NUMERIC_TYPES.add("java.lang.Double");
    NUMERIC_TYPES.add("java.lang.Long");
    NUMERIC_TYPES.add("java.lang.Integer");
    NUMERIC_TYPES.add("java.lang.Byte");
    NUMERIC_TYPES.add("java.lang.Short");
    NUMERIC_TYPES.add("java.lang.Number");
  }

  public CritterField(final CritterContext context, final Field<JavaClassSource> field) {
    this.context = context;
    source = field;
    getFullType(field.getType());
    if (field.getType().isParameterized()) {
      final List<Type<JavaClassSource>> typeArguments = field.getType().getTypeArguments();
      for (Type<JavaClassSource> typeArgument : typeArguments) {
        shortParameterTypes.add(typeArgument.getName()) ;
        fullParameterTypes.add(typeArgument.getQualifiedName());
      }
    }
  }

  private void getFullType(final Type<JavaClassSource> type) {
    final JavaSource<?> nestedType = source.getOrigin().getNestedType(type.getName());
    fullType = nestedType != null
        ? nestedType.getCanonicalName()
        : type.getQualifiedName();
  }

  @Override
  public int compareTo(final CritterField other) {
    return source.getName().compareTo(other.source.getName());
  }

  public String getName() {
    return source.getName();
  }

  public String getFullType() {
    return fullType;
  }

  public boolean hasAnnotation(final Class<? extends Annotation> aClass) {
    return source.hasAnnotation(aClass);
  }

  public Boolean isContainer() {
    final String qualifiedName = source.getType().getQualifiedName();
    return qualifiedName.equals("java.util.List")
        || qualifiedName.equals("java.util.Set");
  }

  public boolean isNumeric() {
    return NUMERIC_TYPES.contains(source.getType().getQualifiedName());
  }

  public List<String> getParameterTypes() {
    return fullParameterTypes;
  }

  public String getParameterizedType() {
    if(getParameterTypes().isEmpty()) {
      return fullType;
    } else {
      return format("%s<%s>", fullType, Strings.join(fullParameterTypes, ", "));
    }
  }

  public Field<JavaClassSource> getSource() {
    return source;
  }

  public String getFullyQualifiedType() {
    final String qualifiedName = source.getType().getQualifiedName();
    final List<Type<JavaClassSource>> typeArguments = source.getType().getTypeArguments();
    String types = typeArguments.isEmpty()
        ? ""
        : "<" + join(",", typeArguments.stream().map(Type::getQualifiedName).collect(Collectors.toList())) + ">";
    return format("%s%s", qualifiedName, types);
  }

  @Override
  public String toString() {
    return "CritterField{" +
        "source=" + source +
        '}';
  }

  public void build(final CritterClass critterClass, final JavaClassSource criteriaClass) {
    if (getSource().hasAnnotation(Reference.class)) {
      buildReference(critterClass, criteriaClass);
    } else if (hasAnnotation(Embedded.class)) {
      buildEmbed(critterClass, criteriaClass);
    } else {
      buildField(critterClass, criteriaClass);
    }

  }

  public void buildReference(final CritterClass critterClass, final JavaClassSource criteriaClass) {
    criteriaClass.addMethod()
        .setPublic()
        .setName(source.getName())
        .setReturnType(criteriaClass)
        .setBody(format("query.filter(\"%s = \", reference);\n"
            + "return this;", source.getName()))
        .addParameter(source.getType().getQualifiedName(), "reference");
  }

  public void buildEmbed(final CritterClass critterClass, final JavaClassSource criteriaClass) {
    criteriaClass.addImport(Criteria.class);
    String criteriaType;
    if(!shortParameterTypes.isEmpty()) {
      criteriaType = shortParameterTypes.get(0) + "Criteria";
      criteriaClass.addImport(criteriaClass.getPackage() + "." + criteriaType);
    } else {
      final Type<JavaClassSource> type = source.getType();
      criteriaType = type.getQualifiedName() + "Criteria";
      criteriaClass.addImport(source.getType().getQualifiedName());
    }
    final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
        .setPublic()
        .setName(source.getName())
        .setReturnType(criteriaType);
    method
        .setBody(format("return new %s(query, \"%s\");", method.getReturnType().getName(), source.getName()));
  }

  public void buildField(final CritterClass critterClass, final JavaClassSource criteriaClass) {
    final String qualifiedName = critterClass.getSourceClass().getQualifiedName();
    criteriaClass.addImport(qualifiedName);
    criteriaClass.addImport(fullType);
    criteriaClass.addImport(Criteria.class);
    criteriaClass.addImport(FieldEndImpl.class);
    criteriaClass.addImport(QueryImpl.class);

    String name = "\"" + source.getName() + "\"";
    if(getSource().getOrigin().hasAnnotation(Embedded.class) || context.isEmbedded(getSource().getOrigin())) {
      name = "prefix + " + name;
    }

    criteriaClass.addMethod()
        .setPublic()
        .setName(source.getName())
        .setReturnType(format("%s<%s, %s, %s>", TypeSafeFieldEnd.class.getName(), criteriaClass.getQualifiedName(),
            critterClass.getSourceClass().getQualifiedName(), fullType))
        .setBody(format("return new TypeSafeFieldEnd<%s, %s, %s>(this, query, %s);",
            criteriaClass.getName(), critterClass.getSourceClass().getName(), fullType, name));

    final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
        .setName(source.getName())
        .setPublic()
        .setReturnType(Criteria.class)
        .setBody(format("return new TypeSafeFieldEnd<%s, %s, %s>(this, query, %s).equal(value);",
            criteriaClass.getName(), critterClass.getSourceClass().getName(), fullType, name));
    method.addParameter(getParameterizedType(), "value");
  }

  public String mappedName() {
    String name = getName();
    name = extract(name, Property.class);
    name = extract(name, Embedded.class);

    return name;
  }

  private String extract(String name, final Class<? extends Annotation> ann) {
    final org.jboss.forge.roaster.model.Annotation<JavaClassSource> annotation = source.getAnnotation(ann);
    return annotation != null && annotation.getStringValue("value") != null
        ? annotation.getStringValue("value")
        : name;
  }
}
