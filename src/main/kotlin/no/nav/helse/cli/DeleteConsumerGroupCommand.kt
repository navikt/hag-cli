package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory

internal class DeleteConsumerGroupCommand : Command {
    override val name = "delete_consumer_group"

    override fun usage() {
        println("Usage: $name <consumer group>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required consumerGroup")
        val consumerGroup = args[0]
        val client = factory.adminClient()
        client.deleteConsumerGroups(listOf(consumerGroup))
        println("========================================================")
        println("Consumer group $consumerGroup deleted")
        println("========================================================")
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
