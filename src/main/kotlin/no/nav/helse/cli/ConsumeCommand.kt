package no.nav.helse.cli

import no.nav.helse.cli.operations.findOffsets
import no.nav.helse.cli.operations.getPartitions
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.admin.OffsetSpec
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

internal class ConsumeCommand : Command {
    override val name = "consume"

    override fun usage() {
        println("Usage: $name <topic> <event_name> <timestamp> [text to search]")
        println("Prints all events matching the given type")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or timestamp arg")
        val topic = args[0]
        val eventName = args[1].split(",")
        val time = args.getOrNull(2)?.let { LocalDateTime.parse(args[2]) } ?: LocalDateTime.now()
        val timestamp = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val searchString = args.getOrNull(3)
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from $time with timestamp = $timestamp")
        val adminClient = factory.adminClient()

        val partitions = getPartitions(adminClient, topic)
        val offsetsForTime = findOffsets(adminClient, partitions, OffsetSpec.forTimestamp(timestamp))
        RapidsCliApplication(factory)
            .apply {
                JsonRiver(this)
                    .validate { _, node, _ -> node.path("@event_name").asText() in eventName }
                    .validate { record, _, _ -> searchString?.let { record.value().contains(it) } ?: true }
                    .onMessage { record, node ->
                        println("#${record.partition()}, offset ${record.offset()} - ${node.path("@id").asText()} --> $node")
                    }
            }
            .partitionsAssignedFirstTime { consumer, partitionsAssigned ->
                partitionsAssigned.forEach { partition ->
                    val offset = offsetsForTime.getValue(partition)
                    if (offset > 0) consumer.seek(partition, offset)
                }
            }
            .start(groupId, listOf(topic))
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
