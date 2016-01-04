package com.antwerkz.critter;

import com.antwerkz.critter.criteria.BaseCriteria;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jetbrains.dokka.DocumentationNode;
import org.jetbrains.dokka.DocumentationReference;
import org.jetbrains.dokka.DocumentationReference.Kind;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.query.Query;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

public class CritterClass {
    private static final Logger LOG = Logger.getLogger(CritterClass.class.getName());

    private DocumentationNode node;
    private final CritterContext context;
    private final File sourceFile;

    private final String name;

    private final String packageName;

    private final boolean embedded;

    private final List<CritterField> fields = new ArrayList<>();
    private final String descriptorName;

    public CritterClass(DocumentationNode node, final CritterContext context) {
        this.node = node;
        this.context = context;
        embedded = hasAnnotation(Embedded.class);
        final String pkg = node.references(Kind.Owner)
                               .stream()
                               .findFirst()
                               .get().getTo().getName();
        packageName = pkg + ".criteria";
        name = node.getName() + "Criteria";
        descriptorName = node.getName() + "Descriptor";

        String fileName = String.format("%s/%s.%s", pkg.replace('.', '/'), node.getName(), context.getOutputFormat());
        final Optional<String> first = context.getSourceRoots().stream()
                                              .filter(root -> new File(root, fileName).exists())
                                              .findFirst();
        if(first.get() == null) {
            throw new IllegalStateException("Can't found source file for " + fileName);
        }
        sourceFile = new File(first.get(), fileName);
    }

    public CritterClass(CritterContext context, final File sourceFile, final JavaType<?> type) {
        this.context = context;
        this.sourceFile = sourceFile;
//        sourceClass = (JavaClassSource) type;
        name = null; //sourceClass.getName();
        descriptorName = null; //sourceClass.getName();
        embedded = false; //sourceClass.hasAnnotation(Embedded.class);
        packageName = null; //sourceClass.getPackage() + ".criteria";
    }

    public JavaClassSource getSourceClass() {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getPackage() {
        return packageName;
    }

    public void build() {
        try {
            if (hasAnnotation(Entity.class) || hasAnnotation(Embedded.class)) {
                buildCriteria();
                buildDescriptor();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, format("Failed to generate criteria class for %s: %s", getName(), e.getMessage()), e);
        }
    }

    private void buildDescriptor() {
        final JavaClassSource descriptorClass = Roaster.create(JavaClassSource.class);
        descriptorClass.setPackage(packageName).setName(descriptorName);

        final File outputFile = new File(context.getOutputDirectory(), descriptorClass.getQualifiedName().replace('.', '/') + ".java");
        if (outputFile.lastModified() < getLastModified()) {
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

    private void buildCriteria() {
        final JavaClassSource criteriaClass = Roaster.create(JavaClassSource.class);
        criteriaClass.setPackage(packageName).setName(name);

        final File outputFile = new File(context.getOutputDirectory(), criteriaClass.getQualifiedName().replace('.', '/') + ".java");
        if (outputFile.lastModified() < getLastModified()) {
            if (!hasAnnotation(Embedded.class)) {
                criteriaClass.setSuperType(String.format("%s<%s>", BaseCriteria.class.getName(), name));
                final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
                                                                          .setPublic()
                                                                          .setName(getName())
                                                                          .setBody(format("super(ds, %s.class);", name));
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
            if (!hasAnnotation(Embedded.class)) {
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
        long modified = sourceFile.lastModified();
//        final CritterClass superClass = context.get(sourceClass.getSuperType());
//        if (superClass != null) {
//            modified = Math.min(modified, superClass.getLastModified());
//        }
        return modified;
    }

    public List<CritterField> getFields() {
/*
        if (fields == null) {
            fields = sourceClass.getFields().stream()
                                .filter((f) -> !f.isStatic())
                                .map(f -> new CritterField(context, f))
                                .sorted((l, r) -> l.getName().compareTo(r.getName()))
                                .collect(toList());
            final CritterClass superClass = context.get(sourceClass.getSuperType());
            if (superClass != null) {
                fields.addAll(superClass.getFields());
            }
        }
*/
        return fields;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    private boolean hasAnnotation(final Class<?> aClass) {
        return node.references(Kind.Annotation).stream()
                   .map(a -> a.getTo().getName())
                   .anyMatch(fqcn -> fqcn.equals(aClass.getSimpleName()));
    }

    @Override
    public String toString() {
        return packageName + "." + name;
    }
}
