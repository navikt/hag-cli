package no.nav.helse.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpResponse.BodySubscribers
import kotlin.system.exitProcess

internal class CheckVersionCommand : Command {
    private companion object {
        private const val REPO = "navikt/hag-cli"
        private val mapper = jacksonObjectMapper()
        // velg en exitkode som bash-script kan forvente betyr at
        // versjonen er utdatert. Bør ikke være 0 og 1 ettersom de er i bruk fra før
        private const val EXIT_CODE_NEW_VERSION = 10
    }

    override val name = "check_version"

    override fun usage() {
        println("Usage: $name [--download [output filename]")
        println("--download causes new version to be downloaded and dumped to stdout, or the given output filename")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
        val downloadNewVersion = args.getOrNull(0) == "--download"
        val outputFilename = args.getOrNull(1)

        val repo = this::class.java.`package`.implementationVendor ?: REPO
        val response = httpClient.send(HttpRequest.newBuilder(URI.create("https://api.github.com/repos/$repo/releases/latest")).GET().build()) {
            BodySubscribers.mapping(BodySubscribers.ofByteArray()) { mapper.readTree(it) }
        }
        val selfVersion: String? = this::class.java.`package`.implementationVersion
        val version = response.body().path("tag_name").asText()
        val downloadUrl = response.body().path("assets").first { it.path("name").asText() == "app.jar" }.path("browser_download_url").asText()
        val isOutdated = selfVersion != version

        if (!downloadNewVersion) {
            println("Self version: $selfVersion")
            println("Tag: $version")
            if (isOutdated) {
                println("New version available!")
                println("Run with `--download` (and an optional filename) to automatically download the new binary")
            } else {
                println("You have the latest version")
            }
        } else if (isOutdated) {
            val outputStream = outputFilename?.let { FileOutputStream(it) } ?: System.out
            val request = HttpRequest
                .newBuilder(URI.create(downloadUrl))
                .GET()
                .build()
            val fileResponse = httpClient.send(request, BodyHandlers.ofInputStream())
            fileResponse.body().use { inputStream ->
                outputStream.use { inputStream.transferTo(it) }
            }
        }
        if (isOutdated) exitProcess(EXIT_CODE_NEW_VERSION)
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
