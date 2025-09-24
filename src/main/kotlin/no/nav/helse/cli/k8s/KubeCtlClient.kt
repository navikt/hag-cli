package no.nav.helse.cli.k8s

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1SecretList

typealias KubeSecret = Map<String, ByteArray>

fun Map<String, ByteArray>.value(key: String): String = this[key]?.decodeToString() ?: throw RuntimeException("Feil ved parsing, mangler nøkkel: $key")

fun Map<String, ByteArray>.rawBytevalue(key: String): ByteArray = this[key] ?: throw RuntimeException("Feil ved parsing, mangler nøkkel: $key")

fun SecretType.getNameString() =
    when (this) {
        SecretType.Aiven -> "aiven"
    }

class KubeCtlClient(
    context: String = "dev-gcp"
) {
    private val apiClient: ApiClient = hentDevGcpKubeConfig(context)
    private val api: CoreV1Api

    init {
        Configuration.setDefaultApiClient(apiClient)
        api = CoreV1Api()
    }

    private fun getServiceNames(): List<String> {
        val secrets: V1SecretList =
            api.listNamespacedSecret(
                "helsearbeidsgiver",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )

        return secrets.items
            .map { it.metadata?.name.toString() }
    }

    private fun getServiceWithName(name: String): List<String> = getServiceNames().filter { it.contains(name) }

    fun getServices(secretType: SecretType) = getServiceWithName(secretType.getNameString())

    fun getSecrets(serviceName: String): KubeSecret {
        try {
            val response =
                api.readNamespacedSecret(
                    serviceName,
                    "helsearbeidsgiver",
                    null
                )
            return response.data?.mapValues { it.value } ?: throw RuntimeException("No data found in secret")
        } catch (e: Exception) {
            throw RuntimeException("Failed to get secret value: ${e.message}", e)
        }
    }
}
