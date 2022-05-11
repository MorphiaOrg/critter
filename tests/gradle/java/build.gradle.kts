plugins {
    id("dev.morphia.critter") version "4.2.0-SNAPSHOT"
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
    testImplementation("com.antwerkz.bottlerocket:bottlerocket:${findProperty("bottlerocket.version")}")
}

tasks {
   test {
        useTestNG()
    }

    critter {
        force = true
        outputType = "java"
    }
}

tasks.withType(JavaCompile::class.java) {
    options.compilerArgs = listOf("-parameters")
    options.forkOptions.executable = "javac"
}