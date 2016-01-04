package com.antwerkz.critter;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.io.File;
import java.io.FileNotFoundException;

public class CritterTask extends SourceTask {
  private CritterPluginExtension extension;

  private File outputDirectory;

  private FileCollection critterClasspath;

  public CritterPluginExtension getExtension() {
    return extension;
  }

  public void setExtension(final CritterPluginExtension extension) {
    this.extension = extension;
  }

  @InputFiles
  public FileCollection getCritterClasspath() {
    return critterClasspath;
  }

  public void setCritterClasspath(final FileCollection critterClasspath) {
    this.critterClasspath = critterClasspath;
  }

  @OutputDirectory
  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(final File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  @TaskAction
  public void generate() {
    CritterContext context = new CritterContext();
    getSource().getFiles().forEach(file -> {
      if (!file.getName().endsWith("Criteria.java")) {
        final JavaType<?> type;
        try {
          type = Roaster.parse(file);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
        if (type instanceof JavaClassSource) {
          context.add(new CritterClass(context, file, type));
        }
      }
    });

    context.getClasses().stream()
        .forEach(critterClass -> critterClass.build(outputDirectory));
  }
}
