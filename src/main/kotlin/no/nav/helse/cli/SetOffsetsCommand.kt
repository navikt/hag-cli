package no.nav.helse.cli

import no.nav.helse.cli.operations.getOffsets
import no.nav.helse.cli.operations.getPartitions
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.system.exitProcess

internal class SetOffsetsCommand : Command {
    override val name = "set_offsets"

    override fun usage() {
        println("Usage: $name <consumer group> <topic>")
        println("Manually sets offsets for a consumer group")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required consumerGroup or topic arg")
        val consumerGroup = args[0]
        val topic = args[1]
        val client = factory.adminClient()

        val partitions = getPartitions(client, topic).sortedBy { it.partition() }
        val currentOffsets = getOffsets(client, listOf(consumerGroup))

        println("$topic consists of ${partitions.size} partitions.")
        println("Enter a new offset for each partition, leave blank to leave unaltered, type 'E' for EARLIEST or type 'L' for LATEST")

        val groupId = "hag-cli-${Random.nextInt()}"
        val app = RapidsCliApplication(factory)

        val startOffsets = mutableMapOf<TopicPartition, Long>()
        val endOffsets = mutableMapOf<TopicPartition, Long>()
        app.start(groupId, listOf(topic)) { consumer ->
            startOffsets.putAll(consumer.beginningOffsets(partitions))
            endOffsets.putAll(consumer.endOffsets(partitions))
            app.stop()
        }

        val offsets = partitions.mapNotNull { partition ->
            val earliest = startOffsets.getValue(partition)
            val latest = endOffsets.getValue(partition)
            val current = currentOffsets[consumerGroup]?.get(partition)?.offset()
            print("Enter a new offset for partition#${partition.partition()} (current is: $current, earliest is $earliest, latest is $latest): ")

            val output = readln().takeUnless { it.isBlank() }?.let { input ->
                input.toLongOrNull() ?: when (input.first()) {
                    'E', 'e' -> earliest
                    'L', 'l' -> latest
                    else -> null
                }
            }
            output?.let { partition to it }
        }
        if (offsets.isEmpty()) return println("No changes")

        println("You have entered:")
        offsets.forEach { (partition, offset) ->
            println("\tPartition #${partition.partition()}: $offset")
        }
        println("Is this correct? [y/N]")
        val answer = readln()

        if (answer.lowercase() != "y") return println("Aborting")

        println("Setting offsets")
        client.alterConsumerGroupOffsets(consumerGroup, offsets.associate { (partition, offset) ->
            partition to OffsetAndMetadata(offset, "Offset set manually via cli at ${LocalDateTime.now()}")
        }).all().get()

        println("Current offsets:")
        getOffsets(client, listOf(consumerGroup))
            .getValue(consumerGroup)
            .forEach { (partition, offsetAndMetadata) ->
                println("#${partition.partition()}: ${offsetAndMetadata.offset()}${offsetAndMetadata.metadata()?.takeUnless { it.isBlank() }?.let { " ($it)" } ?: ""}")
            }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
