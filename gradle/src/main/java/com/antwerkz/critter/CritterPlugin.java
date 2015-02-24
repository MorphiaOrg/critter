package com.antwerkz.critter;

import java.io.File;
import java.util.concurrent.Callable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_CONFIGURATION_NAME;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class CritterPlugin implements Plugin<Project> {
  public static final String CRITTER_CONFIGURATION_NAME = "critter";

  @Override
  public void apply(final Project project) {
    project.getPlugins().apply(JavaPlugin.class);
    final CritterPluginExtension extension = project.getExtensions().create("critter", CritterPluginExtension.class);

    Configuration critterConfiguration = project.getConfigurations().create(CRITTER_CONFIGURATION_NAME).setVisible(false)
            .setTransitive(false).setDescription("The Critter libraries to be used for this project.");
    project.getConfigurations().getByName(COMPILE_CONFIGURATION_NAME).extendsFrom(critterConfiguration);
  
    project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
      public void execute(SourceSet sourceSet) {
        final String taskName = sourceSet.getTaskName("generate", "CritterSource");
        CritterTask task = project.getTasks().create(taskName, CritterTask.class);
        task.setDescription(String.format("Processes the %s java files for critter.",
            sourceSet.getName()));

        task.setSource(sourceSet.getAllJava().getFiles());
        task.setExtension(extension);

        task.getConventionMapping().map("critterClasspath",
            () -> project.getConfigurations().getByName(CRITTER_CONFIGURATION_NAME).copy().setTransitive(true));

        final String outputDirectoryName = String.format("%s/generated-src/critter/%s",
            project.getBuildDir(), sourceSet.getName());
        final File outputDirectory = new File(outputDirectoryName);
        task.setOutputDirectory(outputDirectory);
        sourceSet.getJava().srcDir(outputDirectory);

        project.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(taskName);
      }
    });
  }
}
