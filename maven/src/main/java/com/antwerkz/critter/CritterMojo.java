package com.antwerkz.critter;

import com.antwerkz.critter.dokka.CritterGenerator;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.dokka.AnalysisEnvironment;
import org.jetbrains.dokka.DocumentationModule;
import org.jetbrains.dokka.DocumentationOptions;
import org.jetbrains.dokka.DokkaGenerator;
import org.jetbrains.dokka.DokkaLogger;
import org.jetbrains.dokka.DokkaMessageCollector;
import org.jetbrains.dokka.MainKt;
import org.jetbrains.dokka.SourceLinkDefinition;
import org.jetbrains.dokka.Utilities.DokkaModule;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.jetbrains.dokka.MainKt.buildDocumentationModule;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CritterMojo extends AbstractMojo {
    @Parameter(defaultValue = "src/main/java", readonly = true, required = true)
    private File sourceDirectory;

    @Parameter(property = "critter.output.directory", defaultValue = "${project.build.directory}/generated-sources/critter",
        readonly = true, required = true)
    private File outputDirectory;

    @Parameter(property = "critter.criteria.package")
    private String criteriaPackage;

    @Parameter(property = "critter.force", defaultValue = "false")
    private boolean force;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        project.addCompileSourceRoot(outputDirectory.getPath());
        final List<String> sources = findSourceFiles();
        final CritterDokkaLogger logger = new CritterDokkaLogger();
        final List<File> classpath = emptyList();

        dokkaGenerator(logger, Collections.emptyList(), sources,
                           Collections.emptyList(), sources, "critter",
                           outputDirectory.getAbsolutePath(), "java", emptyList(), false);
/*
        new DokkaGenerator(logger, Collections.emptyList(), sources,
                           Collections.emptyList(), sources, "critter",
                           outputDirectory.getAbsolutePath(), "java", emptyList(), false)
            .generate();
*/


        //    CritterContext context = new CritterContext(criteriaPackage, force);
        //    final DirectoryWalker walker = new DirectoryWalker();
        //    walker.setBaseDir(sourceDirectory);
        //    walker.setIncludes(singletonList("**/*.java"));
        //
        //    try {
        //      walker.addDirectoryWalkListener(new DirectoryWalkListener() {
        //        @Override
        //        public void directoryWalkStarting(final File basedir) {
        //        }
        //
        //        @Override
        //        public void directoryWalkStep(final int percentage, final File file) {
        //          if (!file.getName().endsWith("Criteria.java")) {
        //            final JavaType<?> type;
        //            try {
        //              type = Roaster.parse(file);
        //            } catch (FileNotFoundException e) {
        //              e.printStackTrace(System.out);
        //              throw new RuntimeException(e.getMessage(), e);
        //            }
        //            if (type instanceof JavaClassSource) {
        //              context.add(type.getPackage(), new CritterClass(context, file, type));
        //            }
        //          }
        //        }
        //
        //        @Override
        //        public void directoryWalkFinished() {
        //        }
        //
        //        @Override
        //        public void debug(final String message) {
        //        }
        //      });
        //      walker.scan();
        //      context.getClasses().stream()
        //          .forEach(critterClass -> critterClass.build(outputDirectory));
        //    } catch (Throwable e) {
        //      e.printStackTrace(System.out);
        //    }
    }

    private List<String> findSourceFiles() {
        List<String> files = new ArrayList<>();

        final DirectoryWalker walker = new DirectoryWalker();
        walker.setBaseDir(sourceDirectory);
        walker.setIncludes(Collections.singletonList("**/*.java"));

        try {
            walker.addDirectoryWalkListener(new DirectoryWalkListener() {
                @Override
                public void directoryWalkStarting(final File basedir) {
                }

                @Override
                public void directoryWalkStep(final int percentage, final File file) {
                    if (!file.getName().endsWith("Criteria.java")) {
                        files.add(file.getAbsolutePath());
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
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }

        return files;
    }

    private static class CritterDokkaLogger implements DokkaLogger {
        @Override
        public void error(@NotNull final String s) {

        }

        @Override
        public void info(@NotNull final String s) {

        }

        @Override
        public void warn(@NotNull final String s) {

        }
    }

    private void dokkaGenerator(final CritterDokkaLogger logger, final List<File> classpath, final List<String> sources,
                                final List<String> samples, final List<String> includes, final String name, final String outputDir,
                                final String outputFormat, final List<SourceLinkDefinition> sourceLinks, final boolean skipDeprecated) {

        DocumentationOptions options = new DocumentationOptions(outputDir, outputFormat, true, false, true, skipDeprecated, sourceLinks);

        final AnalysisEnvironment environment = createAnalysisEnvironment(logger, classpath, sources, samples);
        Injector injector = Guice.createInjector(new DokkaModule(environment, options, logger));

        final DocumentationModule documentation = buildDocumentationModule(injector, name, (PsiFile it) -> true, includes);

        final CritterContext context = injector.getInstance(CritterContext.class);
        context.setOutputDirectory(outputDir);
        context.addSourceRoots(project.getCompileSourceRoots());
        context.setOutputFormat(outputFormat);

        injector.getInstance(CritterGenerator.class).buildPages(documentation.getMembers());

    }

    private AnalysisEnvironment createAnalysisEnvironment(final DokkaLogger logger, final List<File> classpath,
                                                          final List<String> sources, final List<String> samples) {
        AnalysisEnvironment environment = new AnalysisEnvironment(new DokkaMessageCollector(logger));

        environment.addClasspath(PathUtil.getJdkClassesRoots());
        classpath.forEach(environment::addClasspath);

        environment.addSources(sources);
        environment.addSources(samples);

        return environment;
    }

}