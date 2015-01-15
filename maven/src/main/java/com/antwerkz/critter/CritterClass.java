package com.antwerkz.critter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.antwerkz.critter.criteria.BaseCriteria;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

public class CritterClass {
  private final CritterContext context;

  private String name;

  private String packageName;

  private String fullyQualifiedName;

  private final Set<CritterField> fields = new TreeSet<>();

  private final List<CritterField> embeds = new ArrayList<>();

  private List<CritterField> references = new ArrayList<>();

  private JavaClassSource source;

  public CritterClass(CritterContext context, final File file) {
    this.context = context;
    try {
      source = (JavaClassSource) Roaster.parse(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    packageName = source.getPackage();
    name = source.getName();
    fullyQualifiedName = source.getQualifiedName();
    source.getFields().stream().forEach(new FieldConsumer());
  }

  private void add(final CritterContext context, final JavaClassSource javaClass,
      final FieldSource<JavaClassSource> field) {
    final CritterField critterField = new CritterField(context, javaClass, field);

    if (field.getAnnotation(Reference.class) != null) {
      references.add(critterField);
    } else if (field.getAnnotation(Embedded.class) != null) {
      embeds.add(critterField);
    } else {
      fields.add(critterField);
    }
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public JavaClassSource build() {
    final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
    javaClass.setPackage(packageName).setName(source.getName() + "Criteria");
    javaClass.setSuperType(BaseCriteria.class.getName() + "<" + source.getQualifiedName() + ">" );

    for (CritterField field : fields) {
      field.build(javaClass);
    }
    return javaClass;
  }

  private class FieldConsumer implements Consumer<FieldSource<JavaClassSource>> {
    @Override
    public void accept(final FieldSource<JavaClassSource> field) {
      add(context, source, field);
    }
  }
}
