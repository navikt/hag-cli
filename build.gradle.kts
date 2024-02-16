plugins {
    kotlin("jvm") version "1.6.10"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val rapidsAndRiversCli = "1.94f5da1"
val junitJupiterVersion = "5.10.2"

dependencies {
    api("com.github.navikt:rapids-and-rivers-cli:$rapidsAndRiversCli")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    named<Jar>("jar") {
        archiveFileName.set("app.jar")
        manifest {
            attributes["Main-Class"] = "no.nav.helse.cli.ApplicationKt"
            archiveVersion.orNull?.also { version ->
                attributes["Implementation-Version"] = version
            }
            project.findProperty("repository")?.also { repo ->
                attributes["Implementation-Vendor"] = repo
            }
        }

        // unzips all runtime dependencies (jars) and add the files
        // to the application jar
        from(configurations.runtimeClasspath.get().map(::zipTree)) {
            // prints a warning to console to tell about duplicate files
            duplicatesStrategy = DuplicatesStrategy.WARN
            // don't copy any file matching logback.xml;
            filesMatching("logback.xml") { exclude() }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.4.1"
    }
}
