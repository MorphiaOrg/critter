package dev.morphia.critter

import dev.morphia.critter.Critter.generateCodecs
import dev.morphia.critter.Critter.generateCriteria
import dev.morphia.critter.Critter.outputType
import dev.morphia.critter.Critter.scan
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class CritterMojo : AbstractMojo() {
    @Parameter
    private var sourceDirectories = setOf("src/main/java", "src/main/kotlin")

    @Parameter(
        property = "critter.output.directory",
        defaultValue = "\${project.build.directory}/generated-sources/critter",
        readonly = true,
        required = true
    )
    private lateinit var outputDirectory: File

    @Parameter(property = "critter.codecs", defaultValue = "true")
    private var generateCodecs: Boolean = true

    @Parameter(property = "critter.criteria.package")
    private var criteriaPackage: String? = null

    @Parameter(property = "critter.force", defaultValue = "false")
    private var force: Boolean = false

    @Parameter(property = "critter.output.type", name = "outputType", required = true)
    lateinit var outputType: String

    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject
    override fun execute() {
        project.addCompileSourceRoot(outputDirectory.path)
        scan(project.basedir, sourceDirectories, criteriaPackage, force, outputType(outputType), outputDirectory)
        generateCriteria()
        if (generateCodecs) {
            generateCodecs()
        }
    }
}
