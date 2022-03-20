package no.nav.helse.cli

import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.Config
import no.nav.rapids_and_rivers.cli.OnPremConfig
import java.io.File
import java.util.*


fun resolveConfigFromProperties(filename: String): Config {
    val properties = Properties().apply { load(File(filename).bufferedReader()) }
    return when (val config: String = properties.getProperty("config", "N/A")) {
        "aiven" -> aivenConfig(properties)
        "onprem" -> onpremConfig(properties)
        else -> throw RuntimeException("Invalid config selected: $config")
    }
}

private fun Properties.getRequiredProperty(prop: String) = requireNotNull(getProperty(prop)?.takeUnless(CharSequence::isBlank)) {
    "Missing required property: $prop"
}

private fun aivenConfig(properties: Properties): Config {
    return AivenConfig(
        brokers = properties.getRequiredProperty("aiven.brokers.url").split(","),
        truststorePath = properties.getRequiredProperty("aiven.truststore.path"),
        truststorePw = properties.getRequiredProperty("aiven.truststore.password"),
        keystorePath = properties.getRequiredProperty("aiven.keystore.path"),
        keystorePw = properties.getRequiredProperty("aiven.keystore.password")
    )
}

private fun onpremConfig(properties: Properties): Config {
    return OnPremConfig(
        brokers = properties.getRequiredProperty("onprem.brokers.url").split(","),
        username = properties.getRequiredProperty("onprem.username"),
        password = properties.getRequiredProperty("onprem.password"),
        truststorePath = properties.getRequiredProperty("onprem.truststore.path"),
        truststorePw = properties.getRequiredProperty("onprem.truststore.password")
    )
}
