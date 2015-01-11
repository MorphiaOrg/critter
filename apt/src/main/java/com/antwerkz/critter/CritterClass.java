package com.antwerkz.critter;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.NotSaved;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;

public class CritterClass {
    private final ProcessingEnvironment processingEnv;
    private final Template template;
    private String criteriaName;
    private String name;
    private String packageName;
    private String fullyQualifiedName;
    private final Set<CritterField> fields = new TreeSet<>();
    private final Set<CritterField> embeddeds = new TreeSet<>();
    private final Set<CritterField> references = new TreeSet<>();

    public CritterClass(final ProcessingEnvironment processingEnv, final Template template, final TypeElement typeElement) {
        this.processingEnv = processingEnv;
        this.template = template;
        packageName = getPackageName(typeElement);
        name = typeElement.getQualifiedName().toString().replace(packageName + ".", "");
        criteriaName = name.replace('.', '_') + "Criteria";
        fullyQualifiedName = typeElement.getQualifiedName().toString();
        readFields(typeElement);
    }

    public String getCriteriaName() {
        return criteriaName;
    }

    public Set<CritterField> getEmbeddeds() {
        return embeddeds;
    }

    public Set<CritterField> getFields() {
        return fields;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<CritterField> getReferences() {
        return references;
    }

    private void readFields(TypeElement typeElement) {
        while (typeElement != null) {
            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
            for (Element enclosedElement : enclosedElements) {
                if (enclosedElement instanceof VariableElement) {
                    VariableElement field = (VariableElement) enclosedElement;
                    if (!field.getModifiers().contains(Modifier.STATIC)) {
                        if (validField(field)) {
                            fields.add(new CritterField(field));
                        } else if (embedded(field)) {
                            embeddeds.add(new CritterField(field));
                        } else if (reference(field)) {
                            references.add(new CritterField(field));
                        }
                    }
                }
            }
            typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeElement.getSuperclass());
        }
    }

    private String getPackageName(TypeElement typeElement) {
        QualifiedNameable enclosingElement = (QualifiedNameable) typeElement.getEnclosingElement();
        while (!(enclosingElement instanceof PackageElement)) {
            enclosingElement = (QualifiedNameable) enclosingElement.getEnclosingElement();
        }
        return enclosingElement.getQualifiedName().toString();
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

    public File getSourceFile() {
        return new File(getOutputDirectory(), format("%s/criteria/%s.java", packageName.replace('.', '/'), criteriaName));
    }

    private String getOutputDirectory() {
        String outputDirectory = processingEnv.getOptions().get("outputDirectory");
        return outputDirectory == null ? "src/main/java" : outputDirectory;
    }

    public void generate() throws TemplateException, IOException {
        File source = getSourceFile();
        source.getParentFile().mkdirs();
        final File tempFile = File.createTempFile(name, ".java");
        tempFile.deleteOnExit();
        try (PrintWriter out = new PrintWriter(tempFile)) {
            template.process(this, out);
            Files.move(tempFile.toPath(), source.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public String toString() {
        return "CritterClass{" +
            "fullyQualifiedName='" + fullyQualifiedName + '\'' +
            ", packageName='" + packageName + '\'' +
            ", name='" + name + '\'' +
            ", criteriaName='" + criteriaName + '\'' +
            ", fields=" + fields +
            ", embeddeds=" + embeddeds +
            ", references=" + references +
            ", template=" + template.getName() +
            '}';
    }
}
