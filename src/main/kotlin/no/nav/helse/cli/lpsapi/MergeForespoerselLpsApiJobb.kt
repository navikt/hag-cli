package no.nav.helse.cli.lpsapi

import Environment

fun executeJob(environment: Environment) {
    val localDbRepository = LocalDbRepository(environment.tableName)
    val producerFactory = producerFactory(environment.name)
    val forespoersler = localDbRepository.getNyeForespoersler()
    forespoersler.forEach { uuid ->
        sendForespoerselOppdaterForespurtData(producerFactory, uuid.toString())
        localDbRepository.updateStatusByForespoerselId(uuid, ImportStatus.SENDT)
    }
}
fun main() {
    try {
        executeJob(Environment.PROD)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
