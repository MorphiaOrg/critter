project.version = org.apache.maven.model.io.xpp3.MavenXpp3Reader().read(file("pom.xml").inputStream())
    .parent.version

plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.5.10"
    id("dev.morphia.critter") version "4.2.0-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

sourceSets.test {
    java.srcDirs("src/test/kotlin")
}

dependencies {
    implementation("dev.morphia.morphia:morphia-core:${findProperty("morphia.version")}")
    testImplementation("org.testng:testng:7.4.0")
    testImplementation("com.antwerkz.bottlerocket:bottlerocket:0.14")
}

tasks {
    test {
        useTestNG()
    }

    critter {
        force = true
    }
}
