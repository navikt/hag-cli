package no.nav.helse.cli

import no.nav.helse.cli.operations.getOffsets
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import java.lang.Thread.sleep
import kotlin.math.round
import kotlin.math.roundToInt

internal class FlowrateCommand : Command {
    override val name = "flowrate"

    override fun usage() {
        println("Usage: $name <comma-separated consumer groups>")
        println("Calculates a flowrate for each consumer group. The flow rate is calculated using the committed offsets as a basis ")
        println("for how many messages each consumer group is processing.")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required consumerGroup arg")
        val consumerGroups = args[0].split(",")
        val client = factory.adminClient()
        printFlowrate(client, consumerGroups)
    }

    override fun verify(factory: ConsumerProducerFactory) {}

    private var lastObservation = emptyMap<String, Map<TopicPartition, OffsetAndMetadata>>()
    private val observations = mutableMapOf<String, MutableList<Int>>()
    private val maximumObservation = mutableMapOf<String, Int>()
    private val intervalMs = 2000L
    private fun printFlowrate(client: AdminClient, consumerGroups: List<String>) {
        print("Collecting initial data, please hold")
        while (true) {
            val offsets = getOffsets(client, consumerGroups)
            printResult(lastObservation, offsets)
            lastObservation = offsets
            sleep(intervalMs)
        }
    }

    private fun printResult(before: Map<String, Map<TopicPartition, OffsetAndMetadata>>, after: Map<String, Map<TopicPartition, OffsetAndMetadata>>) {
        if (before.isEmpty()) return
        print("\r")
        calculateDelta(before, after)
            .map { (consumerGroup, messages) ->
                val flowrate = messages * 1000L / intervalMs.toDouble()
                consumerGroup to round(flowrate).toInt()
            }
            .onEach { (consumerGroup, flowratePerSecond) ->
                observations.getOrPut(consumerGroup) { mutableListOf() }.add(flowratePerSecond)
                maximumObservation.compute(consumerGroup) { _, old ->
                    old?.let { maxOf(flowratePerSecond, old) } ?: flowratePerSecond
                }
            }
            .joinToString { (consumerGroup, flowratePerSecond) ->
                val maxima = maximumObservation.getValue(consumerGroup).toString().padStart(4)
                val avg = observations.getValue(consumerGroup).average().roundToInt().toString().padStart(4)
                "$consumerGroup: ${flowratePerSecond.toString().padStart(4)} msgs/s [max: $maxima msgs/s, avg: $avg msgs/s]"
            }.also {
                print(it)
            }
    }

    private fun calculateDelta(before: Map<String, Map<TopicPartition, OffsetAndMetadata>>, after: Map<String, Map<TopicPartition, OffsetAndMetadata>>) =
        after.mapValues { (consumerGroup, offsets) ->
            offsets.mapNotNull { (partition, offsetAndMetadata) ->
                before[consumerGroup]?.get(partition)?.offset()?.let { beforeValue -> offsetAndMetadata.offset() - beforeValue }
            }.sum()
        }.toSortedMap()
}
