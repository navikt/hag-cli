package no.nav.helse.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt

internal class ObserveCommand : Command, JsonRiver.JsonValidationSuccessListener {
    override val name = "observe"
    private val mapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun usage() {
        println("Usage: $name <topic>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required topic arg")
        val topic = args[0]
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from LATEST")
        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this).apply {
                    validate { _, node, _ ->
                        node.hasNonNull("@event_name")
                    }
                }.onMessage(this@ObserveCommand)
            }
            .start(groupId, listOf(topic))
    }

    private val events = mutableMapOf<String, Int>()
    override fun onMessage(record: ConsumerRecord<String, String>, node: JsonNode) {
        events.compute(node.path("@event_name").asText()) { _, oldValue ->
            oldValue?.let { it + 1 } ?: 1
        }
        printStatistics(events)
    }

    private var lastPrintTime = 0L
    private fun printStatistics(events: Map<String, Int>) {
        val now = System.currentTimeMillis()
        if ((abs(now - lastPrintTime)) < 1000) return
        lastPrintTime = now

        val biggest = events.keys.maxOf { it.length }
        println()
        events
            .toList()
            .sortedByDescending { (_, count) -> count }
            .forEach { (event, count) -> println(event.padEnd(biggest + 4) + " : $count") }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
