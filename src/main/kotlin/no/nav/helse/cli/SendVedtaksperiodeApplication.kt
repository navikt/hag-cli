package no.nav.helse.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.UUID
import jdk.internal.joptsimple.internal.Messages.message
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.header
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.textInput
import no.nav.rapids_and_rivers.cli.Config
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord

fun main(args: Array<String>) {
    if (args.isEmpty()) return help()

    val config: Config
    if (args[0].equals("prod", true)) {
        println("Starter applikasjon i Prod!")
        config = resolveConfigFromProperties("config/prod-aiven.properties")
    } else if (args[0].equals("dev", true)) {
        println("Starter applikasjon i Dev!")
        config = resolveConfigFromProperties("config/dev-aiven.properties")
    } else {
        return help()
    }

    val factory = ConsumerProducerFactory(config)

    SendVedtaksperiodeApplication(factory, args[0]).startServer()
}

private fun help() {
    println("Ugyldig argument: Forventet 'prod' eller 'dev'.")
}

class SendVedtaksperiodeApplication(
    private val factory: ConsumerProducerFactory,
    private val env: String
) {
    fun startServer() {
        embeddedServer(Netty, port = 8080) {
            routing {
                get("/") {
                    call.respondHtml {
                        body {
                            header {
                                style = "text-align: center; margin-top: 50px;"
                                h1 { +"Send VedtaksperiodeId til Kafka i $env" }
                            }
                            form(action = "/send", method = FormMethod.post) {
                                label { +"VedtaksperiodeId" }
                                textInput(name = "vedtaksperiodeid") {
                                    placeholder = "Skriv inn en eller flere UUID-er, separert med komma"
                                    style = "width: 400px;"
                                }
                                br
                                button(type = ButtonType.submit) { +"Send" }
                            }
                        }
                    }
                }
                post("/send") {
                    val params = call.receiveParameters()
                    val input = params["vedtaksperiodeid"] ?: ""
                    val uuids = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val result = sendToKafka(uuids)
                    call.respondHtml {
                        body {
                            if (result) {
                                p {
                                    style = "color: green"
                                    +"OK"
                                }
                            } else {
                                p {
                                    style = "color: red"
                                    +"Feilet"
                                }
                            }
                            a(href = "/") { +"Tilbake" }
                        }
                    }
                }
            }
        }.start(wait = true)
    }

    fun sendToKafka(uuids: List<String>): Boolean {
        if (uuids.isEmpty()) {
            println("Ingen UUID-er oppgitt.")
            return false
        }
        try {
            factory.createProducer().use { producer ->
                uuids.forEach { uuid ->
                    val validateAndCleanUUID = validateAndCleanUUID(uuid)
                    val message = """{
                   "@behov" : "HENT_FORESPOERSLER_FOR_VEDTAKSPERIODE_ID",
                   "vedtaksperiode_id" : "$validateAndCleanUUID"
               }"""
                    println(message)
                    producer.send(ProducerRecord("helsearbeidsgiver.pri", "key", message)).get().also { recordMetadata ->
                        println("========================================================")
                        println("Record produced to partition #${recordMetadata.partition()} with offset ${recordMetadata.offset()}")
                        println("========================================================")
                    }
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun validateAndCleanUUID(uuid: String): UUID {
        var uuidCleaned = uuid
        if (uuid.isNotEmpty() && uuid.contains("'")) {
            println("Fjerner enkelt anførselstegn fra UUID: $uuid")
            uuidCleaned = uuid.removeSingleQuotes()
        }
        return UUID.fromString(uuidCleaned)
    }
}

fun String.removeSingleQuotes(): String = this.replace("'", "")
