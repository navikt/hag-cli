package no.nav.helse.cli.lpsapi

import java.sql.Connection
import java.sql.DriverManager
import kotlin.uuid.ExperimentalUuidApi

fun main() {
    try {
        executeJob()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalUuidApi::class)
fun executeJob() {
    val localDbRepository = LocalDbRepository()
    val sendtForespoerseler = localDbRepository.getSendtForespoerseler()
    sendtForespoerseler.forEach { forespoersel ->
        lpsapiConnection().use { conn ->
            val statement =
                conn.prepareStatement(
                    "SELECT * from forespoersel where nav_referanse_id=?::uuid and vedtaksperiode_id=?::uuid and status=?"
                )
            statement.setObject(1, forespoersel.forespoerselId,java.sql.Types.OTHER )
            statement.setObject(2, forespoersel.vedtaksperiodeId, java.sql.Types.OTHER )
            statement.setObject(3, forespoersel.status, java.sql.Types.OTHER )
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    println("Forespoersel med ID ${forespoersel.forespoerselId} finnes i LPSAPI med status ${forespoersel.status}.")
                    localDbRepository.updateStatus(forespoersel.forespoerselId, ImportStatus.OK)
                } else {
                    println("Forespoersel med ID ${forespoersel.forespoerselId} finnes ikke i LPSAPI.")
                    localDbRepository.updateStatus(forespoersel.forespoerselId, ImportStatus.FEILET)
                }
            }
        }
    }
}

fun lpsapiConnection(): Connection {
    val url = "jdbc:postgresql://localhost:55559/hag-lps"
    val user = "mehdi.zare@nav.no"
    val password = ""
    val conn = DriverManager.getConnection(url, user, password)
    conn.autoCommit = false
    conn.createStatement().execute("SET TRANSACTION READ ONLY")
    return conn
}
