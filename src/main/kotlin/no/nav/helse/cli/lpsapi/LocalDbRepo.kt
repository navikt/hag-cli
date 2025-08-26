@file:OptIn(ExperimentalUuidApi::class)

package no.nav.helse.cli.lpsapi

import ImportStatus
import java.util.UUID
import kotlin.use
import kotlin.uuid.ExperimentalUuidApi
import localConnection

@OptIn(ExperimentalUuidApi::class)
class LocalDbRepository(var tableName: String) {
    fun getVedtaksperiodeId(): ArrayList<String> {
        val vedtaksperiodeIdlist = arrayListOf<String>()
        localConnection().use { conn ->
            val statement = conn.createStatement()
            val resultSet = statement.executeQuery("select distinct vedtaksperiode_id from $tableName where imported = 'NY'")
            while (resultSet.next()) {
                val id = resultSet.getString("vedtaksperiode_id")
                vedtaksperiodeIdlist.add(id)
            }
        }
        return vedtaksperiodeIdlist
    }

    fun updateStatus(
        vedtaksperiodeIdListe: ArrayList<String>,
        importStatus: ImportStatus
    ) {
        localConnection().use { conn ->
            val statement =
                conn.prepareStatement(
                    "UPDATE $tableName SET imported = ? WHERE vedtaksperiode_id = ?::uuid"
                )
            vedtaksperiodeIdListe.forEach { vedtaksperiodeId ->
                statement.setString(1, importStatus.name)
                statement.setString(2, vedtaksperiodeId)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun getSendtForespoerseler(): ArrayList<Foresepoersel> {
        arrayListOf<Foresepoersel>().also { list ->
            localConnection().use { conn ->
                val statement = conn.createStatement()
                val resultSet = statement.executeQuery("select * from $tableName where imported = 'SENDT'")
                while (resultSet.next()) {
                    val forespoerselId = resultSet.getString("forespoersel_id")
                    val vedtaksperiodeId = resultSet.getString("vedtaksperiode_id")
                    val status = resultSet.getString("status")
                    val imported = resultSet.getString("imported")
                    list.add(Foresepoersel(UUID.fromString(forespoerselId), UUID.fromString(vedtaksperiodeId), status, imported))
                }
            }
            return list
        }
    }

    fun updateStatus(
        forespoerselId: UUID,
        importStatus: ImportStatus
    ) {
        localConnection().use { conn ->
            val statement =
                conn.prepareStatement(
                    "UPDATE $tableName SET imported = ? WHERE forespoersel_id = ?"
                )
            statement.setString(1, importStatus.name)
            statement.setObject(2, forespoerselId, java.sql.Types.OTHER)

            statement.executeUpdate()
        }
    }
}

data class Foresepoersel
    constructor(
        val forespoerselId: UUID,
        val vedtaksperiodeId: UUID,
        val status: String,
        val imported: String
    )
