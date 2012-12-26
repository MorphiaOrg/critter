package com.antwerkz.critter;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.NotSaved;
import com.google.code.morphia.annotations.Transient;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CriteriaBuilder extends AbstractProcessor {

    private ProcessingEnvironment env;

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
        String enclosingElement = typeElement.getEnclosingElement().toString();
        if(name.indexOf('.') != -1) {
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
        String outputDirectory = env.getOptions().get("outputDirectory");
        return outputDirectory == null ? "src/main/java" : outputDirectory;
    }

    private void getFields(TypeElement typeElement, TreeMap<String, Object> map) {
        Set<Field> fields = new TreeSet<>();
        Set<Field> embeddeds = new TreeSet<>();
        while (typeElement != null) {
            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
            for (Element enclosedElement : enclosedElements) {
                if (enclosedElement instanceof VariableElement) {
                    VariableElement field = (VariableElement) enclosedElement;
                    if (!field.getModifiers().contains(Modifier.STATIC)) {
                        if (validField(field)) {
                            fields.add(new Field(field.asType().toString(), field.getSimpleName().toString()));
                        }
                        if (embedded(field)) {
                            embeddeds.add(new Field(encodeEmbedName(field), field.getSimpleName().toString()));
                        }
                    }

                }
            }
            TypeMirror superclass = typeElement.getSuperclass();
            typeElement = (TypeElement) env.getTypeUtils().asElement(superclass);
        }
        map.put("fields", fields);
        map.put("embeddeds", embeddeds);
    }

    private String encodeEmbedName(VariableElement field) {
        TypeMirror typeMirror = field.asType();
        String[] parts = typeMirror.toString().split("\\.");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if(builder.length() != 0) {
                if(i == parts.length - 2) {
                    builder.append(".criteria.");
                } else if(i == parts.length - 1) {
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
        QualifiedNameable enclosingElement = (QualifiedNameable) typeElement.getEnclosingElement();
        while (!(enclosingElement instanceof PackageElement)) {
            enclosingElement = (QualifiedNameable) enclosingElement.getEnclosingElement();
        }
        return enclosingElement.getQualifiedName().toString();
    }

    private boolean validField(VariableElement field) {
        Class[] types = {NotSaved.class, Transient.class, Embedded.class};
        for (Class type : types) {
            if (field.getAnnotation(type) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean embedded(VariableElement field) {
        Set<Modifier> modifiers = field.getModifiers();
        return !modifiers.contains(Modifier.STATIC) && field.getAnnotation(Embedded.class) != null;
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
