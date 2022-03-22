package no.nav.helse.cli

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.File

internal class ProduceCommand : Command {
    override val name = "produce"
    private val mapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun usage() {
        println("Usage: $name <topic> <kafka record key> <path to json file>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 3) throw RuntimeException("Missing required key arg and file arg")
        val topic = args[0]
        val key = args[1]
        // ensure contents are valid json
        val contents = mapper.readTree(File(args[2]))
        factory.createProducer().also { producer ->
            producer.send(ProducerRecord(topic, key, contents.toString())).get().also { recordMetadata ->
                println("==========================================")
                println("Record produced to partition #${recordMetadata.partition()} with offset ${recordMetadata.offset()}")
                println("==========================================")
            }
        }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
