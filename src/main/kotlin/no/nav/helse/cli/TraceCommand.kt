package no.nav.helse.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.cli.operations.findOffsets
import no.nav.helse.cli.operations.getPartitions
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.LinkedList
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess

internal class TraceCommand : Command {
    private companion object {
        private const val DEFAULT_DEPTH = 3
    }
    override val name = "trace"
    private val mapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun usage() {
        println("Usage: $name <topic> <id> [<depth>] [<from timestamp>]")
        println("If <depth> is unset, the default is to trace a message with depth=$DEFAULT_DEPTH")
        println("If <from timestamp> is unset, the default is to look back last two hours")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or id arg")
        val topic = args[0]
        val messageId = args[1]
        val depth = args.getOrNull(2)?.toIntOrNull() ?: DEFAULT_DEPTH
        val time = args.getOrNull(3)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now().minusHours(2)
        val timestamp = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from $time with timestamp = $timestamp")
        val adminClient = factory.adminClient()

        val partitions = getPartitions(adminClient, topic)
        val latestOffsets = findOffsets(adminClient, partitions, OffsetSpec.latest())
        val offsetsForTime = findOffsets(adminClient, partitions, OffsetSpec.forTimestamp(timestamp))
        val upToDate = latestOffsets.mapValues { false }.toMutableMap()

        var rootMessage: Message? = null
        Runtime.getRuntime().addShutdownHook(Thread {
            println(rootMessage?.toString())
        })
        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this)
                    .validate { _, _, _ -> rootMessage == null }
                    .validate { _, node, _ -> node.path("@id").asText() == messageId }
                    .onMessage { record, node ->
                        println("Found message at partition=${record.partition()}, offset = ${record.offset()}")
                        rootMessage = Message(record, node, messageId)
                    }
                JsonRiver(this)
                    .validate { _, node, _ -> true == rootMessage?.erBarnAv(node.path("@forårsaket_av").path("id").asText(), depth) }
                    .onMessage { record, node ->
                        val parentId = node.path("@forårsaket_av").path("id").asText()
                        val message = Message(record, node, node.path("@id").asText())
                        rootMessage?.leggTil(parentId, message)
                    }
                val shutdownRiver = JsonRiver(this)
                shutdownRiver.onMessage { record, _ ->
                        upToDate.compute(TopicPartition(record.topic(), record.partition())) { topicPartition, _ ->
                            record.offset() >= latestOffsets.getValue(topicPartition)
                        }

                        if (upToDate.values.all { it }) {
                            println("Whole topic read, exiting")
                            unregister(shutdownRiver)
                            stop()
                        }
                    }
            }
            .partitionsAssignedFirstTime { consumer, partitionsAssigned ->
                partitionsAssigned.forEach { partition ->
                    consumer.seek(partition, offsetsForTime.getValue(partition))
                }
            }
            .start(groupId, listOf(topic))
    }

    private class Message(
        private val record: ConsumerRecord<String, String>,
        private val node: JsonNode,
        private val id: String
    ) {
        private val children = mutableListOf<Message>()

        internal fun erBarnAv(otherId: String, depth: Int): Boolean {
            if (depth < 0) return false
            if (this.id == otherId) return true
            return children.any { it.erBarnAv(otherId, depth - 1) }
        }

        internal fun leggTil(parentId: String, message: Message): Boolean {
            if (this.id == parentId) return children.add(message)
            return children.any { it.leggTil(parentId, message) }
        }

        private fun eventName(): String {
            if (node.hasNonNull("@behov")) return "behov om ${node.path("@behov").map(JsonNode::asText).joinToString()}"
            return node.path("@event_name").asText()
        }

        override fun toString() = toString(0)

        private fun toString(depth: Int): String {
            return "\t".repeat(depth) + "> ${eventName()}: $id (partition ${record.partition()}, offset ${record.offset()}${children.joinToString(separator = "") { "\n${it.toString(depth + 1) }" }}"
        }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
