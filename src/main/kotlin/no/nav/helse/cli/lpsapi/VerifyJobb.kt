package no.nav.helse.cli.lpsapi

import Environment
import ImportStatus
import java.sql.Connection
import java.sql.DriverManager

fun main() {
    try {
        executeJob()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun executeJob() {
    val localDbRepository = LocalDbRepository(Environment.PROD.tableName)
    val sendtForespoerseler = localDbRepository.getSendtForespoerseler()
    lpsapiConnection().use { conn ->
        val statement =
            conn.prepareStatement(
                "SELECT * FROM forespoersel WHERE nav_referanse_id=? AND vedtaksperiode_id=? AND status=?"
            )
        sendtForespoerseler.forEach { forespoersel ->
            statement.setObject(1, forespoersel.forespoerselId, java.sql.Types.OTHER)
            statement.setObject(2, forespoersel.vedtaksperiodeId, java.sql.Types.OTHER)
            statement.setString(3, forespoersel.status) // if status is String
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    println("Forespoersel med ID ${forespoersel.forespoerselId} finnes i LPSAPI med status ${forespoersel.status}.")
                    localDbRepository.updateStatus(forespoersel.forespoerselId, ImportStatus.OK)
                } else {
                    println("Forespoersel med ID ${forespoersel.forespoerselId} finnes ikke i LPSAPI.")
                    localDbRepository.updateStatus(forespoersel.forespoerselId, ImportStatus.FEILET)
                }
            }
            statement.clearParameters()
        }
    }
}

