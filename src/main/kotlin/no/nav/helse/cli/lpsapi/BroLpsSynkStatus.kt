package no.nav.helse.cli.lpsapi

import java.util.UUID
import kotlin.io.path.Path

fun compareAllForespoerslerStatusOptimized() {
    // Load all data from LPS API first
    val lpsData = mutableMapOf<UUID, Pair<UUID, String>>()
    lpsapiConnection().use { lpsConn ->
        lpsConn.createStatement().use { stmt ->
            val resultSet = stmt.executeQuery("SELECT nav_referanse_id,vedtaksperiode_id, status FROM forespoersel")
            while (resultSet.next()) {
                val id = resultSet.getObject("nav_referanse_id") as UUID
                val status = resultSet.getString("status")
                val vedtaksperiodeId = resultSet.getObject("vedtaksperiode_id") as UUID
                lpsData[id] = Pair(vedtaksperiodeId, status)
            }
        }
    }
    println("Loaded ${lpsData.size} forespoersler from LPS API")

    // Load all matching data from BRO in one query using IN clause
    val broData = mutableMapOf<UUID, String>()
    broConnection().use { broConn ->
        // Build IN clause with UUID values
        val uuidList = lpsData.keys.joinToString(",") { "'$it'" }

        broConn.createStatement().use { stmt ->
            val resultSet =
                stmt.executeQuery(
                    """
                SELECT forespoersel_id, status 
                FROM forespoersel 
                WHERE forespoersel_id IN ($uuidList)
            """
                )
            while (resultSet.next()) {
                val id = resultSet.getObject("forespoersel_id") as UUID
                val status = resultSet.getString("status")
                val normalizedStatus =
                    if (status == "BESVART_SIMBA" || status == "BESVART_SPLEIS") {
                        "BESVART"
                    } else {
                        status
                    }

                broData[id] = normalizedStatus
            }
        }
    }
    println("Found ${broData.size} matching forespoersler in BRO")

    // Compare in memory
    var mismatchCount = 0
    var notFoundCount = 0

    lpsData.forEach { (id, lpsStatus) ->
        val broStatus = broData[id]
        when {
            broStatus == null -> {
                println("No entry found for forespoersel_id $id")
                notFoundCount++
                insertLpsBroStatus(id, "IKKE_FUNNET", lpsStatus.second, false, null)
            }

            broStatus != lpsStatus.second -> {
                println("Status mismatch for forespoersel_id $id: expected $lpsStatus, found $broStatus")
                insertLpsBroStatus(id, broStatus, lpsStatus.second, true, lpsStatus.first)
                mismatchCount++
            }
        }
    }

    println("Summary: $mismatchCount mismatches, $notFoundCount not found")
}

fun main() {
    try {
        compareAllForespoerslerStatusOptimized()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun insertLpsBroStatus(
    forespoerselId: UUID,
    statusbro: String,
    statuslps: String,
    finnesIBro: Boolean,
    vedtaksperiodeId: UUID?
) {
    localConnection().use { conn ->
        val statement =
            conn.prepareStatement(
                "INSERT INTO lps_bro_status (forespoersel_id, status_bro, status_lps, finnes_i_bro,vedtaksperiode_id) VALUES (?::uuid, ?, ?, ?::boolean,?::uuid)"
            )
        statement.setObject(1, forespoerselId, java.sql.Types.OTHER)
        statement.setString(2, statusbro)
        statement.setString(3, statuslps)
        statement.setBoolean(4, finnesIBro)
        statement.setObject(5, vedtaksperiodeId, java.sql.Types.OTHER)
        statement.executeUpdate()
    }
}
