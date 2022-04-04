package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory

internal class ConsumersCommand : Command {
    override val name = "consumers"

    override fun usage() {
        println("Usage: $name <topic>")
        println("Print out a list of all consumer groups which has offsets committed for the given topic")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required topic arg")
        val topic = args[0]
        val client = factory.adminClient()

        println("Hang tight while we collect all consumers…")
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
}
