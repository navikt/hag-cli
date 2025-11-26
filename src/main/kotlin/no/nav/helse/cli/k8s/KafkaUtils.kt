package no.nav.helse.cli.k8s

import no.nav.helse.cli.resolveConfigFromProperties
import no.nav.rapids_and_rivers.cli.Config
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory

fun producerFactory(args: String): ConsumerProducerFactory {
    val config: Config
    if (args.equals("prod", true)) {
        println("Starter applikasjon i Prod!")
        config = resolveConfigFromProperties("config/prod-aiven.properties")
    } else if (args.equals("dev", true)) {
        println("Starter applikasjon i Dev!")
        config = resolveConfigFromProperties("config/dev-aiven.properties")
    } else {
        throw Exception("Ugyldig argument: Forventet 'prod' eller 'dev'.")
    }

    val factory = ConsumerProducerFactory(config)
    return factory
}
