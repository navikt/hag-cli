package no.nav.helse.cli.operations

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

internal fun getOffsets(client: AdminClient, consumerGroups: List<String>): Map<String, Map<TopicPartition, OffsetAndMetadata>> =
    consumerGroups.associateWith { consumerGroup ->
        client.listConsumerGroupOffsets(consumerGroup)
            .partitionsToOffsetAndMetadata()
            .get()
    }
