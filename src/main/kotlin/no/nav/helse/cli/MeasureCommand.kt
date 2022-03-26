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
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess

internal class MeasureCommand : Command {
    override val name = "measure"
    private val mapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun usage() {
        println("Usage: $name <topic> <from timestamp>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or timestamp arg")
        val topic = args[0]
        val time = LocalDateTime.parse(args[1])
        val timestamp = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from $time with timestamp = $timestamp")
        val adminClient = factory.adminClient()

        val partitions = getPartitions(adminClient, topic)
        val latestOffsets = findOffsets(adminClient, partitions, OffsetSpec.latest())
        val offsetsForTime = findOffsets(adminClient, partitions, OffsetSpec.forTimestamp(timestamp))

        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this).apply {
                    validate { _, node, _ -> node.hasNonNull("@event_name") }
                }.onMessage(MessageListener(offsetsForTime, latestOffsets))
            }
            .partitionsAssignedFirstTime { consumer, partitionsAssigned ->
                partitionsAssigned.forEach { partition ->
                    val offset = offsetsForTime.getValue(partition)
                    consumer.seek(partition, offset)
                }
            }
            .start(groupId, listOf(topic))
    }

    private class MessageListener(
        private val offsetsForTime: Map<TopicPartition, Long>,
        private val latestOffsets: Map<TopicPartition, Long>
    ) : JsonRiver.JsonValidationSuccessListener {
        private val progress = latestOffsets.mapValues { (topicPartition, latestOffset) ->
            val diff = latestOffset - offsetsForTime.getValue(topicPartition)
            ProgressBar(diff)
        }
        private val events = mutableMapOf<String, MutableList<Int>>()
        override fun onMessage(record: ConsumerRecord<String, String>, node: JsonNode) {
            val topicPartition = TopicPartition(record.topic(), record.partition())
            progress.getValue(topicPartition).progress(record.offset() - offsetsForTime.getValue(topicPartition))
            events.getOrPut(node.path("@event_name").asText()) { mutableListOf() }.add(record.value().length)
            printProgress()
        }

        private val initialDelay = 3000
        private var lastPrintTime = System.currentTimeMillis() + initialDelay
        private fun printProgress() {
            val now = System.currentTimeMillis()
            if ((abs(now - lastPrintTime)) < 1000) return
            lastPrintTime = now

            // prints overall progress:
            val overall = ProgressBar.progressBar(progress.values)
            print("\rConsuming messages … ${overall.toString(50)}")

            overall.done {
                print("\r${"".repeat(90)}") // clear progress bar
                val largestName = events.keys.maxOf { it.length }
                events
                   .toList()
                   .sortedWith(compareByDescending({ it.second.sum() }))
                   .forEach { (event, sizes) ->
                       println("${event.padEnd(largestName + 4)}: ${sizes.size} messages, summing to ${byteSizeToString(sizes.sum())}")
                   }
                exitProcess(0)
            }

            // prints individual progress:
            /*println()
            progress
                .toSortedMap(compareBy({ it.topic() }, { it.partition() }))
                .forEach { (partition, progress) ->
                    println("#${partition.partition().toString().padStart(2)}: ${progress.toString(50)}")
                }*/
        }

        private fun byteSizeToString(length: Int): String {
            if (length < 1024) return "$length B";
            val zeros = (32 - length.countLeadingZeroBits()) / 10;
            return String.format("%.1f %sB", length.toDouble() / (1L.shl(zeros*10)), " KMGTPE"[zeros])
        }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
