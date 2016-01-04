package com.antwerkz.critter;

import com.google.inject.Singleton;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Singleton
public class CritterContext {
    private final List<String> sourceRoots = new ArrayList<>();
    private Map<String, CritterClass> classes = new HashMap<>();
    private String outputFormat = "java";
    private File outputDirectory;

    public List<String> getSourceRoots() {
        return sourceRoots;
    }

    public void addSourceRoots(List<String> roots) {
        sourceRoots.addAll(roots);
    }

    public void add(CritterClass critterClass) {
        classes.put(format("%s.%s", critterClass.getPackage(), critterClass.getName()), critterClass);
    }

    public CritterClass get(String name) {
        return classes.get(name);
    }

    public Collection<CritterClass> getClasses() {
        return classes.values();
    }

    public boolean isEmbedded(final JavaClassSource clazz) {
        final CritterClass critterClass = get(clazz.getName());
        return critterClass != null && critterClass.isEmbedded();
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(final String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setOutputDirectory(final String directory) {
        this.outputDirectory = new File(directory);
        this.outputDirectory.mkdirs();
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }
}
