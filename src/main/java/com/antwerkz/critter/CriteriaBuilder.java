/**
 * Copyright (C) 2012-2013 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.critter;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.NotSaved;
import com.google.code.morphia.annotations.Reference;
import com.google.code.morphia.annotations.Transient;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import javax.annotation.processing.AbstractProcessor;
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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CriteriaBuilder extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_7;
  }

  @Override
  public Set<String> getSupportedOptions() {
    return Collections.singleton("outputDirectory");
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(Entity.class.getName(), Embedded.class.getName()));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    final TypeElement element = processingEnv.getElementUtils().getTypeElement(Entity.class.getName());
    if (annotations.contains(element)) {
      try {
        Configuration cfg = new Configuration();
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        Template template = cfg.getTemplate("criteria.ftl");
        for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Entity.class))) {
          generate(template, typeElement);
        }
        template = cfg.getTemplate("embedded.ftl");
        for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Embedded.class))) {
          generate(template, typeElement);
        }
        return true;
      } catch (IOException | TemplateException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    } else {
      return false;
    }
  }

  private void generate(Template temp, TypeElement typeElement) throws TemplateException, IOException {
    String pkg = getPackageName(typeElement);
    String name = typeElement.getQualifiedName().toString().replace(pkg + ".", "");
    if (name.indexOf('.') != -1) {
      // nested
      name = name.replace('.', '_');
    }
    TreeMap<String, Object> map = new TreeMap<>();
    map.put("name", name);
    map.put("package", pkg);
    map.put("fqcn", typeElement.getQualifiedName().toString());
    getFields(typeElement, map);
    File source = new File(getOutputDirectory(),
                              String.format("%s/criteria/%sCriteria.java", pkg.replace('.', '/'), name));
    source.getParentFile().mkdirs();
    try (PrintWriter out = new PrintWriter(source)) {
      temp.process(map, out);
    }
  }

  private String getOutputDirectory() {
    String outputDirectory = processingEnv.getOptions().get("outputDirectory");
    return outputDirectory == null ? "src/main/java" : outputDirectory;
  }

  private void getFields(TypeElement typeElement, TreeMap<String, Object> map) {
    Set<Field> fields = new TreeSet<>();
    Set<Field> embeds = new TreeSet<>();
    Set<Field> references = new TreeSet<>();
    while (typeElement != null) {
      List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
      for (Element enclosedElement : enclosedElements) {
        if (enclosedElement instanceof VariableElement) {
          VariableElement field = (VariableElement) enclosedElement;
          if (!field.getModifiers().contains(Modifier.STATIC)) {
            if (validField(field)) {
              fields.add(new Field(field.asType().toString(), field.getSimpleName().toString()));
            } else if (embedded(field)) {
              embeds.add(new Field(encodeEmbedName(field), field.getSimpleName().toString()));
            } else if (reference(field)) {
              references.add(new Field(field.asType().toString(), field.getSimpleName().toString()));
            }
          }
        }
      }
      typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeElement.getSuperclass());
    }
    map.put("fields", fields);
    map.put("embeddeds", embeds);
    map.put("references", references);
  }

  private String encodeEmbedName(VariableElement field) {
    System.out.println("field = [" + field + "]");
    System.out.println("enclosedElements = " + field.getEnclosedElements());
    System.out.println("constantValue = " + field.getConstantValue());
    TypeMirror typeMirror = field.asType();
    ClassType classType = (ClassType) typeMirror;
    List<Type> typeArguments = classType.getTypeArguments();
    String[] parts;
    if(typeArguments.size() == 1) {
      parts = typeArguments.get(0).toString().split("\\.");
    } else {
      parts = typeMirror.toString().split("\\.");
    }
    System.out.println("typeMirror = " + typeMirror);
    System.out.println("typeMirror = " + typeMirror.getClass());
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (builder.length() != 0) {
        if (i == parts.length - 2) {
          builder.append(".criteria.");
        } else if (i == parts.length - 1) {
          builder.append("_");
        } else {
          builder.append(".");
        }
      }
      builder.append(part);
    }
    return builder.toString();
  }

  private String getPackageName(TypeElement typeElement) {
    System.out.println("************ CriteriaBuilder.getPackageName: typeElement = [" + typeElement + "]");
    QualifiedNameable enclosingElement = (QualifiedNameable) typeElement.getEnclosingElement();
    while (!(enclosingElement instanceof PackageElement)) {
      enclosingElement = (QualifiedNameable) enclosingElement.getEnclosingElement();
    }
    String s = enclosingElement.getQualifiedName().toString();
    System.out.println("************ s = " + s);
    return s;
  }

  private boolean validField(VariableElement field) {
    for (Class type : new Class[]{NotSaved.class, Transient.class, Embedded.class, Reference.class}) {
      if (field.getAnnotation(type) != null) {
        return false;
      }
    }
    return true;
  }

  private boolean embedded(VariableElement field) {
    Set<Modifier> modifiers = field.getModifiers();
    com.sun.tools.javac.util.List<Type> typeArguments = ((ClassType) field.asType()).getTypeArguments();

    return !modifiers.contains(Modifier.STATIC) && field.getAnnotation(Embedded.class) != null
               && typeArguments.size() < 2;
  }

  private boolean reference(VariableElement field) {
    Set<Modifier> modifiers = field.getModifiers();
    return !modifiers.contains(Modifier.STATIC) && field.getAnnotation(Reference.class) != null;
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
