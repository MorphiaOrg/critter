package com.antwerkz.critter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.jboss.forge.roaster.model.source.JavaClassSource;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CritterMojo extends AbstractMojo {
  @Parameter(property = "directory", defaultValue = "src/main/java")
  private File directory;

  @Parameter(property = "includes", defaultValue = "**/*.java")
  private String includes;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    CritterContext context = new CritterContext();
//    final FileSet fileSet = new FileSet();
//    fileSet.setDirectory(directory);
//    fileSet.setIncludes(asList(includes.split(",")));
//    fileSet.includes()
    final DirectoryWalker walker = new DirectoryWalker();
    walker.setBaseDir(directory);
    walker.setIncludes(asList(includes.split(",")));

    walker.addDirectoryWalkListener(new DirectoryWalkListener() {
      @Override
      public void directoryWalkStarting(final File basedir) {
      }

      @Override
      public void directoryWalkStep(final int percentage, final File file) {
        if (!file.getName().endsWith("Criteria.java")) {
          context.add(new CritterClass(context, file));
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

    context.getClasses().stream().forEach(new Consumer<CritterClass>() {
      @Override
      public void accept(final CritterClass critterClass) {
        final JavaClassSource criteriaClass = critterClass.build();
        final String fileName = criteriaClass.getQualifiedName().replace('.', '/') + ".java";
        final File file = new File(directory, fileName);
        file.getParentFile().mkdirs();
        try(PrintWriter writer =  new PrintWriter(file)) {
          System.out.printf("Generating %s in to %s\n", criteriaClass.getName(), file);
          writer.println(criteriaClass);
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });
  }
}