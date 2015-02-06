package com.antwerkz.critter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.List;

import com.antwerkz.critter.criteria.BaseCriteria;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.query.Query;

public class CritterClass {
  private final CritterContext context;

  private String name;

  private String packageName;

  private JavaClassSource sourceClass;

  private final boolean embedded;

  private List<CritterField> fields;

  public CritterClass(CritterContext context, final JavaType<?> type) {
    this.context = context;
    sourceClass = (JavaClassSource) type;
    name = sourceClass.getName();
    embedded = sourceClass.hasAnnotation(Embedded.class);
    context.add(this);
  }

  public JavaClassSource getSourceClass() {
    return sourceClass;
  }

  public String getName() {
    return name;
  }

  public String getPackage() {
    return sourceClass.getPackage();
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean hasAnnotation(final Class<? extends Annotation> aClass) {
    return sourceClass.hasAnnotation(aClass);
  }

  public void build(final File directory) {
    if (hasAnnotation(Entity.class) || hasAnnotation(Embedded.class)) {

      final JavaClassSource criteriaClass = Roaster.create(JavaClassSource.class);
      criteriaClass.setPackage(packageName).setName(sourceClass.getName() + "Criteria");

      if (!sourceClass.hasAnnotation(Embedded.class)) {
        criteriaClass.setSuperType(BaseCriteria.class.getName() + "<" + sourceClass.getQualifiedName() + ">");
        final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
            .setPublic()
            .setName(getName())
            .setBody(format("super(ds, %s.class);", sourceClass.getName()));
        method
            .setConstructor(true)
            .addParameter(Datastore.class, "ds");
      } else {
        criteriaClass.addField()
            .setPrivate()
            .setType(Query.class)
            .setName("query");
        criteriaClass.addField()
            .setPrivate()
            .setType("String")
            .setName("prefix");

        final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
            .setPublic()
            .setName(getName())
            .setBody("this.query = query;\nthis.prefix = prefix + \".\";");
        method
            .setConstructor(true)
            .addParameter(Query.class, "query");
        method
            .addParameter(String.class, "prefix");
      }

      for (CritterField field : getFields()) {
        field.build(this, criteriaClass);
      }
      if (!sourceClass.hasAnnotation(Embedded.class)) {
        new UpdaterBuilder(this, criteriaClass);
      }
      generate(criteriaClass, directory);
    }
  }

  private void generate(final JavaClassSource criteriaClass, final File directory) {
    final String fileName = criteriaClass.getQualifiedName().replace('.', '/') + ".java";
    final File file = new File(directory, fileName);
    file.getParentFile().mkdirs();
    try (PrintWriter writer = new PrintWriter(file)) {
      System.out.printf("Generating %s in to %s\n", criteriaClass.getName(), file);
      writer.println(criteriaClass.toString());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  public List<CritterField> getFields() {
    final String name1 = sourceClass.getName();
    if (fields == null) {
      fields = sourceClass.getFields().stream()
          .map(f -> new CritterField(context, f))
          .sorted((l, r) -> l.getName().compareTo(r.getName()))
          .collect(toList());
      final CritterClass superClass = context.get(sourceClass.getSuperType());
      if (superClass != null) {
        fields.addAll(superClass.getFields());
      }
    }
    return fields;
  }

  public boolean isEmbedded() {
    return embedded;
  }

  public void setPackage(final String aPackage) {
    this.packageName = aPackage;
  }

  @Override
  public String toString() {
    return packageName + "." + name;
  }
}
