package no.nav.helse.cli.operations

import org.apache.kafka.clients.admin.AdminClient

internal fun getOffsets(client: AdminClient, consumerGroups: List<String>) =
    consumerGroups.associateWith { consumerGroup ->
        client.listConsumerGroupOffsets(consumerGroup)
            .partitionsToOffsetAndMetadata()
            .get()
    }
