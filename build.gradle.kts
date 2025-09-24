import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.tasks

plugins {
    kotlin("jvm") version "2.2.10"
}

repositories {
    val githubPassword: String? by project
    mavenCentral()
    /* ihht. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
        så plasseres github-maven-repo (med autentisering) før nav-mirror slik at github actions kan anvende førstnevnte.
        Det er fordi nav-mirroret kjører i Google Cloud og da ville man ellers fått unødvendige utgifter til datatrafikk mellom Google Cloud og GitHub
     */
    maven {
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

val rapidsAndRiversCli = "1.8bf080c"
val junitJupiterVersion = "5.12.1"

dependencies {
    api("com.github.navikt:rapids-and-rivers-cli:$rapidsAndRiversCli")
    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.kubernetes:client-java:18.0.1")
    implementation("io.ktor:ktor-server-html-builder-jvm:2.3.7")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.7")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.7")
    implementation("no.nav.helsearbeidsgiver:hag-bakgrunnsjobb:1.0.7")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("21"))
    }
}

tasks {
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

}
tasks.named<Jar>("jar") {
    // Remove digital signature files from META-INF
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
