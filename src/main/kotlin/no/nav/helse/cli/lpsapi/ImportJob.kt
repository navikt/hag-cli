import java.sql.Connection
import java.sql.DriverManager
import no.nav.helse.cli.lpsapi.LocalDbRepository
import no.nav.helse.cli.lpsapi.producerFactory
import no.nav.helse.cli.lpsapi.validateAndCleanUUID
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord

enum class ImportStatus {
    NY,
    SENDT,
    FEILET,
    OK
}

fun main() {
    try {
        executeJob()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun executeJob() {
    val localDbRepository = LocalDbRepository()
    val producerFactory = producerFactory("dev")
    val vedtaksperiodeIdListe = localDbRepository.getVedtaksperiodeId()
    sendToKafka(producerFactory, vedtaksperiodeIdListe)
    localDbRepository.updateStatus(vedtaksperiodeIdListe, ImportStatus.SENDT)
}



private fun sendToKafka(
    producerFactory: ConsumerProducerFactory,
    vedtaksperiodeIdListe: ArrayList<String>
) {
    producerFactory.createProducer().use { producer ->
        vedtaksperiodeIdListe.forEach { uuid ->
            val validateAndCleanUUID = validateAndCleanUUID(uuid)
            val message = """{
                   "@behov" : "HENT_FORESPOERSLER_FOR_VEDTAKSPERIODE_ID",
                   "vedtaksperiode_id" : "$validateAndCleanUUID"
               }"""
            println(message)
            producer.send(ProducerRecord("helsearbeidsgiver.pri", "key", message)).get().also { recordMetadata ->
                println("========================================================")
                println("Record produced to partition #${recordMetadata.partition()} with offset ${recordMetadata.offset()}")
                println("========================================================")
            }
        }
    }
}




fun localConnection(): Connection {
    val url = "jdbc:postgresql://localhost:5432/importfsp"
    val user = "importfsp"
    val password = "importfsp"
    return DriverManager.getConnection(url, user, password)
}
