package no.nav.helse.cli.operations

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.common.TopicPartition

internal fun getPartitions(client: Admin, topic: String) =
    client.describeTopics(listOf(topic))
    .allTopicNames()
    .get()
    .getValue(topic)
    .partitions()
    .map { TopicPartition(topic, it.partition()) }

internal fun findOffsets(client: Admin, partitions: Collection<TopicPartition>, spec: OffsetSpec) =
    client.listOffsets(partitions.associateWith { spec }).all().get().mapValues { it.value.offset() }
