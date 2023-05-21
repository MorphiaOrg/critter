plugins {
    id("dev.morphia.critter") version "4.4.4-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}


dependencies {
    implementation("dev.morphia.morphia:morphia-core:${findProperty("morphia.version")}")
    testImplementation("org.testng:testng:${findProperty("testng.version")}")
    testImplementation("org.testcontainers:mongodb:${findProperty("testcontainers.version")}")
}

tasks {
   test {
        useTestNG()
    }

    critter {
    }
}

tasks.withType(JavaCompile::class.java) {
    options.compilerArgs = listOf("-parameters")
}
