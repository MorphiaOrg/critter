plugins {
    id("dev.morphia.critter") version "4.1.0-SNAPSHOT"
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
    testImplementation("org.testng:testng:7.4.0")
    testImplementation("com.antwerkz.bottlerocket:bottlerocket:0.14")
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
