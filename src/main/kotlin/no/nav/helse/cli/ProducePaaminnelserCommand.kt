package no.nav.helse.cli

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.File
import java.util.UUID

internal class ProducePaaminnelserCommand : Command {
    override val name = "produce-paaminnelser"
    private val mapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun usage() {
        println("Usage: $name <topic> <kafka record key> <path to json file>")
        println("Produces a message onto the topic with the given key and json file.")
    }
    fun String.tilPaaminnelseMelding():String =
        """
        {
            "@event_name" : "MANUELL_ENDRE_PAAMINNELSE",
            "uuid" : "${UUID.randomUUID()}",
            "data" : {
                "forespoerselId": "$this"
            }
        }
        """.trimIndent()


    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 3) throw RuntimeException("Missing required key arg and file arg")
        val topic = args[0]
        val key = args[1]
        // ensure contents are valid json
        val forespoersler = mapper.readValue(File(args[2]), List::class.java).map { it.toString() }


        factory.createProducer().also { producer ->

            forespoersler.forEach { forespoerselId ->
                producer.send(ProducerRecord(topic, key, forespoerselId.tilPaaminnelseMelding())).get().also { recordMetadata ->
                    println("========================================================")
                    println("Record produced to partition #${recordMetadata.partition()} with offset ${recordMetadata.offset()} and forespoerselId: $forespoerselId")
                    println("========================================================")
                }
            }
        }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
