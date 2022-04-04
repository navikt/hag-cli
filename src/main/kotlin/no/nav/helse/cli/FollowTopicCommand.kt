package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import kotlin.random.Random

internal class FollowTopicCommand : Command {
    override val name = "follow_topic"

    override fun usage() {
        println("Usage: $name <topic>")
        println("Prints all messages from a topic")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required topic arg")
        val topic = args[0]
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from LATEST")
        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this).onMessage { record, node ->
                    println("#${record.partition()}, offset ${record.offset()} - ${node.path("@event_name").asText()}: ${node.path("@id").asText()} --> ${node.toString()}")
                }
            }
            .start(groupId, listOf(topic))
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
