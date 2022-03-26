package no.nav.helse.cli

import no.nav.helse.cli.operations.getOffsets
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

internal class ConsumersCommand : Command {
    override val name = "consumers"

    override fun usage() {
        println("Usage: $name <topic>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required topic arg")
        val topic = args[0]
        val client = factory.adminClient()
        val allConsumers = client.listConsumerGroups().valid().get().map { it.groupId() }
        val consumersOfTopic = allConsumers.filter { groupId ->
            client.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata()
                .get()
                .any { (topicPartition, _) -> topicPartition.topic() == topic }
        }

        println("Consumers of topic $topic")
        println(consumersOfTopic.sorted().joinToString(separator = "\n") { "- $it" })
    }

    override fun verify(factory: ConsumerProducerFactory) {}

    private fun printOffsets(client: AdminClient, consumerGroups: List<String>) {
        getOffsets(client, consumerGroups)
            .forEach { (consumerGroup, offsets) ->
                println("Consumer group: $consumerGroup")
                offsets
                .toList()
                .sortedWith(compareBy ({ it.first.topic().hashCode() }, { it.first.partition() }))
                .forEach { (partition, offsetMetadata) ->
                    println("\t${partition.topic()}#${partition.partition()}: ${offsetMetadata.offset()}, metadata: ${offsetMetadata.metadata() ?: "N/A"}")
                }
            }
    }
}
