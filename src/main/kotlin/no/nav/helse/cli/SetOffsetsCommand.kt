package no.nav.helse.cli

import no.nav.helse.cli.operations.getOffsets
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import java.time.LocalDateTime
import kotlin.system.exitProcess

internal class SetOffsetsCommand : Command {
    override val name = "set_offsets"

    override fun usage() {
        println("Usage: $name <consumer group> <topic>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required consumerGroup or topic arg")
        val consumerGroup = args[0]
        val topic = args[1]
        val client = factory.adminClient()

        val partitions = client.describeTopics(listOf(topic))
            .allTopicNames()
            .get()
            .getValue(topic)
            .partitions()
            .map { TopicPartition(topic, it.partition()) }
            .sortedBy { it.partition() }

        val currentOffsets = getOffsets(client, listOf(consumerGroup))

        println("$topic consists of ${partitions.size} partitions.")
        println("Enter a new offset for each partition, or leave blank to leave unaltered")

        val offsets = partitions.mapNotNull { partition ->
            print("Enter a new offset for partition#${partition.partition()} (current is: ${currentOffsets[consumerGroup]?.get(partition)?.offset()}): ")
            readln().takeUnless { it.isBlank() }?.toLongOrNull()?.let {
                partition to it
            }
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
