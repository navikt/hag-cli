package no.nav.helse.cli

import no.nav.helse.cli.operations.findOffsets
import no.nav.helse.cli.operations.getPartitions
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.OffsetSpec
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

internal class OffsetsCommand : Command {
    override val name = "offsets"

    override fun usage() {
        println("Usage: $name <topic> <localdatetime string>")
        println("Gets the offsets which were latest at the specified timestamp")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or timestamp arg")
        val topic = args[0]
        val time = LocalDateTime.parse(args[1])
        val timestamp = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val groupId = "bomli-cli-${Random.nextInt()}"

        val adminClient = factory.adminClient()

        val partitions = getPartitions(adminClient, topic)
        findOffsets(adminClient, partitions, OffsetSpec.forTimestamp(timestamp))
            .entries
            .sortedBy { it.key.partition() }
            .forEach { (partition, offset) ->
                println("Partition #${partition.partition()}: $offset")
            }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
