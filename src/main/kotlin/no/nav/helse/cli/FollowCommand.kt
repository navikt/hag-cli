package no.nav.helse.cli

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import kotlin.random.Random

internal class FollowCommand : Command {
    override val name = "follow"

    override fun usage() {
        println("Usage: $name <topic> <fnr>")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or fnr arg")
        val topic = args[0]
        val fnr = args[1]
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from LATEST")
        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this).apply {
                    validate { record, node, _ ->
                        node.path("fødselsnummer").asText() == fnr
                            || record.key() == fnr
                    }
                }.onMessage { record, node ->
                    println("#${record.partition()}, offset ${record.offset()} - ${node.path("@event_name").asText()}: ${node.path("@id").asText()} --> ${node.toString()}")
                }
            }
            .start(groupId, listOf(topic))
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
