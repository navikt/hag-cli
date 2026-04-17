package no.nav.helse.cli.dialog

import java.util.UUID
import kotlinx.serialization.json.JsonElement
import no.nav.helse.cli.k8s.producerFactory
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.network.Send

val dokumentkobling_topic = "helsearbeidsgiver.dokument-kobling"
val dilaoporten_topic = "helsearbeidsgiver.dialog"

class DialogKlient(
    val factory: ConsumerProducerFactory
) {
    fun sendDokumentkobling(
        dokumentkobling: Dokumentkobling
    ): Boolean {
        val message = dokumentkobling.toJson(Dokumentkobling.serializer())
        return sendMessage(message)
    }

    fun sendToKafka(
        dokumentkobling: DialogMelding
    ): Boolean {
        val message = dokumentkobling.toJson(DialogMelding.serializer())
        return sendMessage(message)
    }

    private fun sendMessage(message: JsonElement): Boolean {
        try {
            factory.createProducer().use { producer ->

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

//    dialogKlient.sendDialog(
//        GravidSoeknadMelding(
//            id = UUID.randomUUID(),
//            orgnr = Orgnr("214398982"),
//            navn = "Test Navn",
//            fnr = "12345678910"
//        )
//    )
    val gravidkravId = UUID.randomUUID()
    val kroniskKravId = UUID.randomUUID()

    // Send Gravid søknad
    dialogKlient.sendToKafka(
        GravidSoeknad(
            id = UUID.randomUUID(),
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345"
        )
    )

    // Send Gravid krav - opprettet
    dialogKlient.sendToKafka(
        GravidKrav(
            id = gravidkravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345"
        )
    )

    // Send Gravid krav - endret
    dialogKlient.sendToKafka(
        GravidKravEndret(
            id = UUID.randomUUID(),
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            forrigeKrav = gravidkravId
        )
    )

    // Send Gravid krav - slettet
    dialogKlient.sendToKafka(
        GravidKravSlettet(
            id = gravidkravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345"
        )
    )

    // Send Kronisk søknad
    dialogKlient.sendToKafka(
        KroniskSoeknad(
            id = UUID.randomUUID(),
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345"
        )
    )

    // Send Kronisk krav - opprettet
    dialogKlient.sendToKafka(
        KroniskKrav(
            id = kroniskKravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345"
        )
    )

    // Send Kronisk krav - endret
    dialogKlient.sendToKafka(
        KroniskKravEndret(
            id = UUID.randomUUID(),
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            forrigeKrav = kroniskKravId
        )
    )

    // Send Kronisk krav - slettet
    dialogKlient.sendToKafka(
        KroniskKravSlettet(
            id = kroniskKravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345"
        )
    )
}

private fun JsonElement.toRecord(): ProducerRecord<String, String> = ProducerRecord(dilaoporten_topic, "key", this.toString())
