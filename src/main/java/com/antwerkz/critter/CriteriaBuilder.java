package com.antwerkz.critter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.NotSaved;
import com.google.code.morphia.annotations.Transient;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class CriteriaBuilder extends AbstractProcessor {

  private ProcessingEnvironment env;

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_7;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Entity.class.getName());
  }

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    env = processingEnv;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    final TypeElement element = env.getElementUtils().getTypeElement(Entity.class.getName());
    if (annotations.contains(element)) {
      try {
        Configuration cfg = new Configuration();
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        Template temp = cfg.getTemplate("criteria.ftl");
        Set<TypeElement> elements = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Entity.class));
        for (TypeElement typeElement : elements) {
          String name = typeElement.getSimpleName().toString();
          String pkg = getPackageName(typeElement);
          TreeMap<String, Object> map = new TreeMap<String, Object>();
          map.put("name", name);
          map.put("package", pkg);
          map.put("fqcn", typeElement.getQualifiedName().toString());
          Set<Field> fields = getFields(typeElement, map);
          File source = new File(
              String.format("src/main/generated/%s/criteria/%sCriteria.java", pkg.replace('.', '/'), name));
          source.getParentFile().mkdirs();
          try (PrintWriter out = new PrintWriter(source)) {
            temp.process(map, out);
          }
        }
        return true;
      } catch (IOException | TemplateException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    } else {
      return false;
    }
  }

  private Set<Field> getFields(TypeElement typeElement, TreeMap<String, Object> map) {
    Set<Field> fields = new TreeSet<>();
    while (typeElement != null) {
      List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
      for (Element enclosedElement : enclosedElements) {
        if (enclosedElement instanceof VariableElement) {
          VariableElement field = (VariableElement) enclosedElement;
          if (validField(field)) {
            fields.add(new Field(field.asType().toString(), field.getSimpleName().toString()));
          }
        }
        map.put("fields", fields);
      }
      TypeMirror superclass = typeElement.getSuperclass();
      typeElement = (TypeElement) env.getTypeUtils().asElement(superclass);
    }
    return fields;
  }

  private String getPackageName(TypeElement typeElement) {
    QualifiedNameable enclosingElement = (QualifiedNameable) typeElement.getEnclosingElement();
    while (!(enclosingElement instanceof PackageElement)) {
      enclosingElement = (QualifiedNameable) enclosingElement.getEnclosingElement();
    }
    return enclosingElement.getQualifiedName().toString();
  }

  private boolean validField(VariableElement field) {
    Set<Modifier> modifiers = field.getModifiers();
    if (modifiers.contains(Modifier.STATIC)) {
      return false;
    }

    Class[] types = { NotSaved.class, Transient.class };
    for (Class type : types) {
      if (field.getAnnotation(type) != null) {
        return false;
      }
    }
    return true;
  }

  public static class Field implements Comparable<Field> {

    public String name;

    public String type;

    public Field(String type, String name) {
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Field {");
      sb.append("name='").append(name).append('\'');
      sb.append(", type='").append(type).append('\'');
      sb.append('}');
      return sb.toString();
    }

    @Override
    public int compareTo(Field o) {
      return name.compareTo(o.name);
    }
  }
}
