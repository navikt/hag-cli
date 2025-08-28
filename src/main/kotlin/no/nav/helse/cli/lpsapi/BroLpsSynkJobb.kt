package no.nav.helse.cli.lpsapi

import java.sql.Connection
import java.util.UUID

fun listDifferentForespoerselIds(): MutableMap<String, Foresepoersel> {
    lpsapiConnection().use { lpsConn ->
        broConnection().use { broConn ->
            val startDate = "2025-04-10"
            val endDate = "2025-08-29"
            val lpsApiForespoersel = getForespoerselIdsByDateRange(lpsConn, "nav_referanse_id", startDate, endDate)
            val lpsIds = lpsApiForespoersel.keys
            val broForespoersel = getForespoerselIdsByDateRange(broConn, "forespoersel_id", startDate, endDate)
            val broIds = broForespoersel.keys
            val subtractedIds = broIds.subtract(lpsIds)
            return broForespoersel.filterKeys { it in subtractedIds }.toMutableMap()
        }
    }
}

fun getForespoerselIdsByDateRange(
    connection: Connection,
    id: String,
    startDate: String,
    endDate: String
): MutableMap<String, Foresepoersel> {
    val sql =
        """
        SELECT $id,vedtaksperiode_id,status
        FROM forespoersel
        WHERE opprettet BETWEEN ?::timestamp AND ?::timestamp
        """.trimIndent()
    connection.prepareStatement(sql).use { statement ->
        statement.setString(1, startDate)
        statement.setString(2, endDate)
        statement.executeQuery().use { resultSet ->
            val ids = mutableMapOf<String, Foresepoersel>()
            while (resultSet.next()) {
                ids[resultSet.getString(1)] =
                    Foresepoersel(
                        UUID.fromString(resultSet.getString(2)),
                        UUID.fromString(resultSet.getString(2)),
                        resultSet.getString(3),
                        "IKKE_IMPORTERT"
                    )
            }
            return ids
        }
    }
}

fun main() {
    var environment = Environment.PROD
    try {
        val differentForespoersler = listDifferentForespoerselIds()
        println("Found ${differentForespoersler.size} different forespoersler")

        localConnection().use { localconnection ->
            localconnection
                .prepareStatement(
                    """
                    INSERT INTO ${environment.tableName} (forespoersel_id, vedtaksperiode_id, status, imported)
                    VALUES (?::uuid, ?::uuid, ?, ?)
                    
                    """.trimIndent()
                ).use { statement ->

                    differentForespoersler.entries.forEach { entry ->
                        println("ForespørselId: ${entry.key}, Status: ${entry.value.status}")
                        val status = entry.value.status
                        val normalizedStatus =
                            if (status == "BESVART_SIMBA" || status == "BESVART_SPLEIS") {
                                "BESVART"
                            } else {
                                status
                            }
                        statement.setString(1, entry.key)
                        statement.setString(2, entry.value.vedtaksperiodeId.toString())
                        statement.setString(3, normalizedStatus)
                        statement.setString(4, entry.value.imported)
                        statement.addBatch()
                    }

                    val results = statement.executeBatch()
                    println("Inserted ${results.sum()} records")
                }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
