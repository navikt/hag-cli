package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.admin.AdminClient

internal class CurrentPartitionsCommand : Command {
    override val name = "current_partitions"

    override fun usage() {
        println("Usage: $name <comma-separated consumer groups>")
        println("Prints assigned partitions for each member of the given consumer groups")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.isEmpty()) throw RuntimeException("Missing required consumerGroup arg")
        val consumerGroups = args[0].split(",")
        val client = factory.adminClient()
        printPartitions(client, consumerGroups)
    }

    override fun verify(factory: ConsumerProducerFactory) {}

    private fun printPartitions(client: AdminClient, consumerGroups: List<String>) {
        client.describeConsumerGroups(consumerGroups)
            .describedGroups()
            .forEach { (consumerGroup, result) ->
                result.get().also { description ->
                    println("Consumer group: $consumerGroup")
                    println("\tAssignor: ${description.partitionAssignor()}")
                    println("\tCoordinator: ${description.coordinator()}")
                    description.members().forEach { member ->
                        print("\tConsumer ID: ${member.consumerId()}: ")
                        println(member.assignment().topicPartitions().joinToString { partition ->
                            "${partition.topic()}#${partition.partition()}"
                        })
                        println("\t\tClient ID: ${member.clientId()}")
                        println("\t\tInstance ID: ${member.groupInstanceId().orElse("N/A")}")
                    }
                    println()
                }
            }
    }
}
