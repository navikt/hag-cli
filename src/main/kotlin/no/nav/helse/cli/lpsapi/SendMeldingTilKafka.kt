package no.nav.helse.cli.lpsapi

import executeJob
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord


fun sendForespoerselOppdaterForespurtData(
    producerFactory: ConsumerProducerFactory,
    uuid: String
) {
    producerFactory.createProducer().use { producer ->

        val validateAndCleanUUID = validateAndCleanUUID(uuid)

        val message = """{
                "notis": "${NotisType.OPPDATER_PAAKREVDE_FELTER}",
                "forespoerselId": "$validateAndCleanUUID"
            }"""

        println(message)
        producer.send(ProducerRecord("helsearbeidsgiver.pri", "key", message)).get().also { recordMetadata ->
            println("========================================================")
            println("Record produced to partition #${recordMetadata.partition()} with offset ${recordMetadata.offset()}")
            println("========================================================")
        }
    }
}

fun main() {
    try {
        val producerFactory = producerFactory(Environment.DEV.name)
        sendForespoerselOppdaterForespurtData(producerFactory, TODO("Skriv inn uuid for forespørsel du vil oppdatere"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
