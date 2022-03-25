package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupIdNotFoundException

internal class DeleteConsumerGroupCommand : Command {
    override val name = "delete_consumer_group"

    override fun usage() {
        println("Usage: $name <consumer group> [optional topic]")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required consumerGroup")
        val consumerGroup = args[0]
        val topic = args.getOrNull(1)
        val client = factory.adminClient()
        if (topic != null) {
            val partitions = client.describeTopics(listOf(topic))
                .allTopicNames()
                .get()
                .flatMap { (topic, topicDescription) ->
                    topicDescription.partitions().map { partitionInfo -> TopicPartition(topic, partitionInfo.partition()) }
                }
                .toSet()
            client.deleteConsumerGroupOffsets(consumerGroup, partitions)
                .also { result ->
                    partitions.forEach { partition ->
                        try {
                            println("Deleting offsets for partition $partition")
                            result.partitionResult(partition).get()
                        } catch (err: Exception) { /* do nothing */ }
                    }
                }
        }
        try {
            println("Deleting consumer group alltogether")
            client.deleteConsumerGroups(listOf(consumerGroup)).all().get()
        } catch (err: Exception) {
            /* do nothing */
        }
        println("========================================================")
        println("Consumer group $consumerGroup deleted")
        println("========================================================")
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
