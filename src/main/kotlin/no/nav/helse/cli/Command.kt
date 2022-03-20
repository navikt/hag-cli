package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory

internal interface Command {
    val name: String
    fun usage()
    fun execute(factory: ConsumerProducerFactory, args: List<String>)
    fun verify(factory: ConsumerProducerFactory)
}
