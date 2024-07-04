package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import kotlin.random.Random

internal class FollowEventCommand : Command {
    override val name = "follow_event"

    override fun usage() {
        println("Usage: $name <topic> <comma-separated list of event_name>")
        println("Prints all events matching the given type")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or timestamp arg")
        val topic = args[0]
        val eventNames = args[1].split(",")
        val groupId = "hag-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from LATEST")
        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this)
                    .validate { _, node, _ -> node.path("@event_name").asText() in eventNames }
                    .onMessage { record, node ->
                        println("#${record.partition()}, offset ${record.offset()} - ${node.path("@id").asText()} --> $node")
                    }
            }
            .start(groupId, listOf(topic))
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
