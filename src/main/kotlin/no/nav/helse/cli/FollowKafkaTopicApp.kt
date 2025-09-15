package no.nav.helse.cli

import Environment
import com.sun.tools.javac.tree.TreeInfo.args
import kotlin.random.Random
import no.nav.helse.cli.lpsapi.producerFactory
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication

val groupId = "hag-cli-${Random.nextInt()}"

fun main(args: Array<String>) {
    var env = Environment.DEV
    if (args.isNotEmpty()) {
        env = Environment.valueOf(args[0].uppercase())
    }
    val factory = producerFactory(env.name)
    followTopic(factory, "helsearbeidsgiver.pri")
}

fun followTopic(
    factory: ConsumerProducerFactory,
    topic: String
) {
    println("Consuming from $topic with consumer group $groupId")
    println("Will NOT be committing any offsets. Starting from LATEST")
    RapidsCliApplication(factory)
        .apply {
            JsonRiver(this).onMessage { record, node ->

                println("key=${record.key()}")
                println("#${record.partition()}, offset ${record.offset()} - ${node.path("@event_name").asText()}: ${node.path("@id").asText()} --> $node")
            }
        }.start(groupId, listOf(topic))
}
