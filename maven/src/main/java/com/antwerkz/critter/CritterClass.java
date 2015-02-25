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
    packageName = sourceClass.getPackage() + ".criteria";
  }

  public JavaClassSource getSourceClass() {
    return sourceClass;
  }

  public String getName() {
    return name;
  }

  public String getPackage() {
    return packageName;
  }

  public void setPackage(final String aPackage) {
    this.packageName = aPackage;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean hasAnnotation(final Class<? extends Annotation> aClass) {
    return sourceClass.hasAnnotation(aClass);
  }

  public void build(final File directory) {
    if (hasAnnotation(Entity.class) || hasAnnotation(Embedded.class)) {

      buildCriteria(directory);
      buildDescriptor(directory);
    }
  }

  private void buildDescriptor(final File directory) {
    final JavaClassSource descriptorClass = Roaster.create(JavaClassSource.class);
    descriptorClass.setPackage(packageName).setName(sourceClass.getName() + "Descriptor");

    final File outputFile = new File(directory, descriptorClass.getQualifiedName().replace('.', '/') + ".java");
    if (context.isForce() || outputFile.lastModified() < getLastModified()) {
      for (CritterField field : getFields()) {
        descriptorClass.addField()
            .setPublic()
            .setStatic(true)
            .setFinal(true)
            .setType(String.class)
            .setName(field.getName())
            .setStringInitializer(field.mappedName());
      }

      generate(descriptorClass, outputFile);
    }
  }

  private void buildCriteria(final File directory) {
    final JavaClassSource criteriaClass = Roaster.create(JavaClassSource.class);
    criteriaClass.setPackage(packageName).setName(sourceClass.getName() + "Criteria");

    final File outputFile = new File(directory, criteriaClass.getQualifiedName().replace('.', '/') + ".java");
    if (context.isForce() || outputFile.lastModified() < getLastModified()) {
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

      generate(criteriaClass, outputFile);
    }
  }

  private void generate(final JavaClassSource criteriaClass, final File file) {
    file.getParentFile().mkdirs();
    try (PrintWriter writer = new PrintWriter(file)) {
      writer.println(criteriaClass.toString());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private long getLastModified() {
    long modified = new File("src/main/java", sourceClass.getQualifiedName().replace('.', '/') + ".java")
        .lastModified();
    final CritterClass superClass = context.get(sourceClass.getSuperType());
    if (superClass != null) {
      modified = Math.min(modified, superClass.getLastModified());
    }
    return modified;
  }

  public List<CritterField> getFields() {
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

  @Override
  public String toString() {
    return packageName + "." + name;
  }
}
