package com.antwerkz.critter;

import java.io.File;
import java.io.FileNotFoundException;

import static java.util.Arrays.asList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CritterMojo extends AbstractMojo {
  @Parameter(property = "critter.source.directory", defaultValue = "src/main/java")
  private File sourceDirectory;

  @Parameter(property = "critter.output.directory", defaultValue = "${project.build.directory}/generated-sources/critter",
      readonly = true, required = true)
  private File outputDirectory;

  @Parameter(property = "critter.includes", defaultValue = "**/*.java")
  private String includes;

  @Parameter(property = "critter.criteria.package")
  private String criteriaPackage;

  @Parameter(property = "critter.force", defaultValue = "false")
  private boolean force;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  public void setCriteriaPackage(final String criteriaPackage) {
    this.criteriaPackage = criteriaPackage;
  }

  public void setSourceDirectory(final File sourceDirectory) {
    this.sourceDirectory = sourceDirectory;
  }

  public void setIncludes(final String includes) {
    this.includes = includes;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    project.addCompileSourceRoot(outputDirectory.getPath());
    CritterContext context = new CritterContext(criteriaPackage, force);
    final DirectoryWalker walker = new DirectoryWalker();
    walker.setBaseDir(sourceDirectory);
    walker.setIncludes(asList(includes.split(",")));

    walker.addDirectoryWalkListener(new DirectoryWalkListener() {
      @Override
      public void directoryWalkStarting(final File basedir) {
      }

      @Override
      public void directoryWalkStep(final int percentage, final File file) {
        if (!file.getName().endsWith("Criteria.java")) {
          final JavaType<?> type;
          try {
            type = Roaster.parse(file);
          } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
          if (type instanceof JavaClassSource) {
            context.add(type.getPackage(), new CritterClass(context, type));
          }
        }
      }

      @Override
      public void directoryWalkFinished() {
      }

      @Override
      public void debug(final String message) {
      }
    });
    walker.scan();

    context.getClasses().stream()
        .forEach(critterClass -> critterClass.build(outputDirectory));
  }
}