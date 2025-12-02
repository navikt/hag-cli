package no.nav.helse.cli.dialog

import java.util.UUID
import kotlin.uuid.Uuid
import kotlinx.serialization.json.JsonElement
import no.nav.helse.cli.k8s.producerFactory
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord

class DialogKlient(
    val factory: ConsumerProducerFactory
) {
    fun sendToKafka(
        melding: Melding
    ): Boolean {
        try {
            factory.createProducer().use { producer ->

                val message = melding.toJson(Melding.serializer())

                println(message)
                producer.send(message.toRecord()).get().also { recordMetadata ->
                    println("========================================================")
                    println("Record produced to partition #${recordMetadata.partition()} with offset ${recordMetadata.offset()}")
                    println("========================================================")
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

fun main() {
    val factory = producerFactory("dev")
    val dialogKlient = DialogKlient(factory)

    dialogKlient.sendToKafka(sykmelding)
    dialogKlient.sendToKafka(sykepengesoeknad)
    dialogKlient.sendToKafka(inntektsmeldingsforespoersel)
    dialogKlient.sendToKafka(inntektsmeldingAvvist)
    dialogKlient.sendToKafka(inntektsmeldingGodkjent)
    dialogKlient.sendToKafka(utgaattInntektsmeldingForespoersel)
    dialogKlient.sendToKafka(inntektsmeldingsforespoerseloppdatert)
}

private fun JsonElement.toRecord(): ProducerRecord<String, String> = ProducerRecord("helsearbeidsgiver.dialog", "key", this.toString())
