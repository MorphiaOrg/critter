repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

project.version = findProperty("critter.version") as String

plugins {
    id("com.gradle.plugin-publish") version "0.13.0"
    kotlin("jvm") version "1.4.31"
    `java-gradle-plugin`
}

dependencies {
    implementation("dev.morphia.critter:critter-generator:${findProperty("critter.version")}")
}

gradlePlugin {
    plugins {
        create("critter") {
            id = "dev.morphia.critter"
            implementationClass = "dev.morphia.critter.CritterPlugin"
            displayName = "Critter Plugin"
            description = "Process Morphia entities and generates type safe criteria classes"
        }
    }
}

pluginBundle {
    mavenCoordinates {
        groupId = "dev.morphia.critter"
        artifactId = "critter-gradle"
    }
    website = "https://morphia.dev/"
    vcsUrl = "https://github.com/MorphiaOrg/critter"
    tags = listOf("morphia", "java", "kotlin")
}
