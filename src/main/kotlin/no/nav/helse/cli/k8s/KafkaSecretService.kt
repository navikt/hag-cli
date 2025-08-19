package no.nav.helse.cli.k8s

import io.ktor.server.plugins.BadRequestException
import java.io.File
import java.nio.file.Paths

enum class SecretType {
    Aiven
}

class KafkaSecretService(
    val secretType: SecretType
) {
    fun hentSecret(
        serviceNavn: String,
        context: String
    ): KubeSecret {
        val kubeCtlClient = KubeCtlClient(context)

        val navnListe = kubeCtlClient.getServices(secretType)

        val targetServiceNavn =
            navnListe
                .find { it.contains(serviceNavn) }
                ?: throw BadRequestException("Fant ikke service. Må være en av disse:\n${navnListe.joinToString("\n")}")

        return kubeCtlClient.getSecrets(targetServiceNavn)
    }
}

fun main() {
    buildKafkaConfig("helsearbeidsgiver-bro-sykepenger", "dev-gcp")
    buildKafkaConfig("helsearbeidsgiver-bro-sykepenger", "prod-gcp")
}

private fun buildKafkaConfig(
    serviceNavn: String,
    context: String = "dev-gcp"
) {
    try {
        val secret = KafkaSecretService(SecretType.Aiven).hentSecret(serviceNavn, context)
        val keystorePath: String
        val truststorePath: String
        if (context == "dev-gcp") {
            keystorePath = lagLokalFil("client.dev.keystore.p12", secret.rawBytevalue("client.keystore.p12"))
            truststorePath = lagLokalFil("client.dev.truststore.jks", secret.rawBytevalue("client.truststore.jks"))
        } else if (context == "prod-gcp") {
            keystorePath = lagLokalFil("client.prod.keystore.p12", secret.rawBytevalue("client.keystore.p12"))
            truststorePath = lagLokalFil("client.prod.truststore.jks", secret.rawBytevalue("client.truststore.jks"))
        } else {
            throw IllegalArgumentException("Ugyldig kontekst: $context. Forventet 'prod-gcp' eller 'dev-gcp'.")
        }

        val kafkaBrokers = secret.value("KAFKA_BROKERS")

        println("Keystore path: $keystorePath")
        println("Truststore path: $truststorePath")
        println("Kafka Brokers: $kafkaBrokers")

        val fileStr =
            readFileFromConfigDir("aiven-example.properties")
                .replace("bokers-url", kafkaBrokers)
                .replace("truststore.jks", truststorePath)
                .replace("keystore.p12", keystorePath)

        println("Read file from config dir: $fileStr")
        if (context == "prod-gcp") {
            println("Prod GCP context detected, using dev-aiven.properties")
            writeStringToConfigDir("prod-aiven.properties", fileStr)
        } else {
            println("Using dev-aiven.properties for context: $context")
            writeStringToConfigDir("dev-aiven.properties", fileStr)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        println("Feil under henting av secret: ${e.message}")
    }
}

fun lagLokalFil(
    navn: String,
    data: ByteArray
): String {
    val keysDir = Paths.get(File(System.getProperty("user.dir"), "keys").absolutePath).toFile()
    if (!keysDir.exists()) {
        keysDir.mkdir()
    }

    val file = File(keysDir, navn)
    file.writeBytes(data)
    return file.absolutePath
}

fun readFileFromConfigDir(fileName: String): String {
    val configDir = Paths.get(System.getProperty("user.dir"), "config").toFile()
    val file = File(configDir, fileName)
    return file.readText(Charsets.UTF_8)
}

fun writeStringToConfigDir(
    fileName: String,
    data: String
): String {
    val configDir = Paths.get(System.getProperty("user.dir"), "config").toFile()
    if (!configDir.exists()) {
        configDir.mkdir()
    }
    val file = File(configDir, fileName)
    file.writeText(data, Charsets.UTF_8)
    return file.absolutePath
}
