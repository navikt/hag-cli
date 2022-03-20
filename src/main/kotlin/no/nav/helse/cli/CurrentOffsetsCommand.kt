package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

internal class CurrentOffsetsCommand : Command {
    override val name = "current_offsets"

    override fun usage() {
        println("Usage: $name <comma-separated consumer groups>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required consumerGroup arg")
        val consumerGroups = args[0].split(",")
        val client = factory.adminClient()
        printOffsets(client, consumerGroups)
    }

    override fun verify(factory: ConsumerProducerFactory) {}

    private fun printOffsets(client: AdminClient, consumerGroups: List<String>) {
        consumerGroups.forEach { consumerGroup ->
            client.listConsumerGroupOffsets(consumerGroup)
                .partitionsToOffsetAndMetadata()
                .get()
                .also { println("Consumer group: $consumerGroup") }
                .toList()
                .sortedWith(compareBy ({ it.first.topic().hashCode() }, { it.first.partition() }))
                .forEach { (partition, offsetMetadata) ->
                    println("\t${partition.topic()}#${partition.partition()}: ${offsetMetadata.offset()}")
                }
        }
    }
}
