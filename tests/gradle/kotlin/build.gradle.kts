plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.6.10"
    id("dev.morphia.critter") version "4.1.3-SNAPSHOT"
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
    testImplementation("org.testng:testng:${findProperty("testng.version")}")
    testImplementation("com.antwerkz.bottlerocket:bottlerocket:${findProperty("bottlerocket.version")}")
}

tasks {
    test {
        useTestNG()
    }

    critter {
        force = true
    }
}
