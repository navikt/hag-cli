package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.common.TopicPartition
import java.lang.Thread.sleep
import kotlin.math.round
import kotlin.math.roundToInt

internal class TopicFlowrateCommand : Command {
    override val name = "topic_flowrate"

    override fun usage() {
        println("Usage: $name <comma-separated topics>")
        println("Calculates a flowrate for each topic. The flow rate is calculated using the committed offsets as a basis ")
        println("for how many messages are being produced on each topic.")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required topics arg")
        val topics = args[0].split(",")
        val client = factory.adminClient()
        val partitions = client.describeTopics(topics)
            .allTopicNames()
            .get()
            .mapValues { (topic, topicDescription) ->
                topicDescription.partitions().map { partitionInfo -> TopicPartition(topic, partitionInfo.partition()) }
            }
        printFlowrate(client, partitions)
    }

    override fun verify(factory: ConsumerProducerFactory) {}

    private var lastObservation = mapOf<TopicPartition, Long>()
    private val observations = mutableMapOf<String, MutableList<Int>>()
    private val maximumObservation = mutableMapOf<String, Int>()
    private val intervalMs = 2000L
    private fun printFlowrate(client: AdminClient, partitions: Map<String, List<TopicPartition>>) {
        print("Collecting initial data, please hold")
        val partitionSpecs = partitions.flatMap { it.value }.associateWith { OffsetSpec.latest() }
        while (true) {
            val offsets = client.listOffsets(partitionSpecs)
                .all()
                .get()
                .mapValues { (partition, result) ->
                    result.offset()
                }
            printResult(lastObservation, offsets)
            lastObservation = offsets
            sleep(intervalMs)
        }
    }

    private fun printResult(before: Map<TopicPartition, Long>, after: Map<TopicPartition, Long>) {
        if (before.isEmpty()) return
        print("\r")
        calculateDelta(before, after)
            .map { (topic, messages) ->
                val flowrate = messages * 1000L / intervalMs.toDouble()
                topic to round(flowrate).toInt()
            }
            .onEach { (topic, flowratePerSecond) ->
                observations.getOrPut(topic) { mutableListOf() }.add(flowratePerSecond)
                maximumObservation.compute(topic) { _, old ->
                    old?.let { maxOf(flowratePerSecond, old) } ?: flowratePerSecond
                }
            }
            .joinToString { (topic, flowratePerSecond) ->
                val maxima = maximumObservation.getValue(topic).toString().padStart(4)
                val avg = observations.getValue(topic).average().roundToInt().toString().padStart(4)
                "$topic: ${flowratePerSecond.toString().padStart(4)} msgs/s [max: $maxima msgs/s, avg: $avg msgs/s]"
            }.also {
                print(it)
            }
    }

    private fun calculateDelta(before: Map<TopicPartition, Long>, after: Map<TopicPartition, Long>) =
        after
            .filterKeys { partition -> before.containsKey(partition) }
            .mapValues { (partition, newValue) ->
                newValue - before.getValue(partition)
            }
            .toList()
            .groupBy { (partition, _) -> partition.topic() }
            .mapValues { (_, offsetPerPartition) -> offsetPerPartition.map { (_, offset) -> offset }.sum() }
            .toSortedMap()
}
