package com.antwerkz.critter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.antwerkz.critter.criteria.BaseCriteria;
import static java.lang.String.format;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.query.Query;

public class CritterClass {
  private final CritterContext context;

  private String name;

  private String packageName;

  private final Set<CritterField> fields = new TreeSet<>();

  private JavaClassSource source;

  private final boolean embedded;

  public CritterClass(CritterContext context, final File file) {
    this.context = context;
    try {
      source = (JavaClassSource) Roaster.parse(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    name = source.getName();
    source.getFields().stream().forEach(new FieldConsumer());
    embedded = source.hasAnnotation(Embedded.class);
    context.add(this);
  }

  private void add(final CritterContext context, final JavaClassSource javaClass,
      final FieldSource<JavaClassSource> field) {
    fields.add(new CritterField(context, javaClass, field));
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public JavaClassSource build() {
    final JavaClassSource criteriaClass = Roaster.create(JavaClassSource.class);
    criteriaClass.setPackage(packageName).setName(source.getName() + "Criteria");

    if (!source.hasAnnotation(Embedded.class)) {
      criteriaClass.setSuperType(BaseCriteria.class.getName() + "<" + source.getQualifiedName() + ">");
      final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
          .setPublic()
          .setName(getName())
          .setBody(format("super(ds, %s.class);", source.getName()));
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

    for (CritterField field : fields) {
      if (field.getSource().hasAnnotation(Reference.class)) {
        field.buildReference(criteriaClass);
      } else if (field.hasAnnotation(Embedded.class)) {
        field.buildEmbed(criteriaClass);
      } else {
        field.buildField(criteriaClass);
      }
    }
    if (!source.hasAnnotation(Embedded.class)) {
      new UpdaterBuilder(this, criteriaClass);
    }
    return criteriaClass;
  }

  public Set<CritterField> getFields() {
    return fields;
  }

  public boolean isEmbedded() {
    return embedded;
  }

  public void setPackage(final String aPackage) {
    this.packageName = aPackage;
  }

  public String getCriteriaPkg() {
    return packageName;
  }

  public void setCriteriaPkg(final String criteriaPkg) {
    this.packageName = criteriaPkg;
  }

  private class FieldConsumer implements Consumer<FieldSource<JavaClassSource>> {
    @Override
    public void accept(final FieldSource<JavaClassSource> field) {
      add(context, source, field);
    }
  }
}
