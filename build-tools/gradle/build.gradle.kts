import org.apache.maven.model.io.xpp3.MavenXpp3Reader

buildscript {
    dependencies {
        classpath("org.apache.maven:maven-model:3.9.1")
    }
}
repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

project.version = MavenXpp3Reader().read(file("pom.xml").inputStream()).parent.version

plugins {
    id("com.gradle.plugin-publish") version "1.1.0"
    kotlin("jvm") version "1.8.10"
    `java-gradle-plugin`
}

dependencies {
    implementation("dev.morphia.critter:critter-generator:${project.version}")
}

gradlePlugin {
    plugins {
        create("critter") {
            id = "dev.morphia.critter"
            implementationClass = "dev.morphia.critter.CritterPlugin"
            displayName = "Critter Plugin"
            description = "Processes Morphia entities and generates type safe criteria classes"
        }
    }
}



publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.morphia.critter"
            artifactId = "critter-gradle"

/*
            pom {
                website.set("https://morphia.dev/")
                vcsUrl = "https://github.com/MorphiaOrg/critter"
                tags = listOf("morphia", "java", "kotlin")
            }
*/

            from(components["java"])
        }
    }
}
