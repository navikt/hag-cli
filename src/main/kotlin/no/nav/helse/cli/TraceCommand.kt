package no.nav.helse.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.cli.operations.findOffsets
import no.nav.helse.cli.operations.getPartitions
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

internal class TraceCommand : Command {
    private companion object {
        private const val DEFAULT_DEPTH = 3
    }
    override val name = "trace"

    override fun usage() {
        println("Usage: $name <topic> <id> [<depth>] [<from timestamp>]")
        println("If <depth> is unset, the default is to trace a message with depth=$DEFAULT_DEPTH")
        println("If <from timestamp> is unset, the default is to look back last two hours")
    }

    override fun execute(factory: ConsumerProducerFactory, args: List<String>) {
        if (args.size < 2) throw RuntimeException("Missing required topic or id arg")
        val topic = args[0]
        val messageId = args[1]
        val depth = args.getOrNull(2)?.toIntOrNull() ?: DEFAULT_DEPTH
        val time = args.getOrNull(3)?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now().minusHours(2)
        val timestamp = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val groupId = "bomli-cli-${Random.nextInt()}"

        println("Consuming from $topic with consumer group $groupId")
        println("Will NOT be committing any offsets. Starting from $time with timestamp = $timestamp")
        val adminClient = factory.adminClient()

        val partitions = getPartitions(adminClient, topic)
        val latestOffsets = findOffsets(adminClient, partitions, OffsetSpec.latest())
        val offsetsForTime = findOffsets(adminClient, partitions, OffsetSpec.forTimestamp(timestamp))
        val upToDate = latestOffsets.mapValues { false }.toMutableMap()

        var rootMessage: Message? = null
        RapidsCliApplication(factory)
            .apply {
                /* capture root message */
                JsonRiver(this)
                    .validate { _, _, _ -> rootMessage == null }
                    .validate { _, node, _ -> node.path("@id").asText() == messageId }
                    .onMessage { record, node ->
                        println("Found message at partition=${record.partition()}, offset = ${record.offset()}")
                        rootMessage = Message(record, node, messageId)
                        println(rootMessage)
                    }
                /* capture all child (or child of children…) messages */
                JsonRiver(this)
                    .validate { _, node, _ ->
                        true == rootMessage?.erBarnAv(node.path("@forårsaket_av").path("id").asText(), depth)
                            || true == rootMessage?.erBarnAv(node.path("@id").asText(), depth)
                            || (node.path("@event_name").asText() in setOf("oppgave_opprettet", "saksbehandler_løsning") && true == rootMessage?.erBarnAv(node.path("hendelseId").asText(), depth))

                    }
                    .onMessage { record, node ->
                        val parentId = node.path("@forårsaket_av").path("id").asText()
                        val message = Message(record, node, node.path("@id").asText())
                        if (true == rootMessage?.leggTil(parentId, message)) {
                            println(message.toString())
                        }
                    }
                /* trigger shutdown when all partitions have been read up to the point collecting in $latestOffsets */
                val shutdownRiver = JsonRiver(this)
                shutdownRiver.onMessage { record, _ ->
                        upToDate.compute(TopicPartition(record.topic(), record.partition())) { topicPartition, _ ->
                            record.offset() >= latestOffsets.getValue(topicPartition)
                        }

                        if (upToDate.values.all { it }) {
                            println("Whole topic read, exiting")
                            unregister(shutdownRiver)
                            stop()
                        }
                    }
            }
            .onShutdown {
                println(rootMessage?.toString())
            }
            .partitionsAssignedFirstTime { consumer, partitionsAssigned ->
                partitionsAssigned.forEach { partition ->
                    consumer.seek(partition, offsetsForTime.getValue(partition))
                }
            }
            .start(groupId, listOf(topic))
    }

    private class Message(
        private val record: ConsumerRecord<String, String>,
        private val node: JsonNode,
        private val id: String
    ) {
        private val parents = mutableListOf<Message>()
        private val depth get() = parents.size

        private val eventName by lazy {
            if (node.hasNonNull("@behov")) behovName(node.path("@behov").joinToString(transform = JsonNode::asText))
            else node.path("@event_name").asText()
        }
        private val extra by lazy {
            if (!node.hasNonNull("@behov")) ""
            else if (node.path("@final").asBoolean()) " (FINAL)"
            else if (!node.path("@løsning").isEmpty) " (DELLØSNING)"
            else " (UTGÅENDE BEHOV)"
        }
        private val produced by lazy { Instant.ofEpochMilli(record.timestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        private val children = mutableListOf<Message>()

        private fun behovName(behov: String) = when (behov) {
            "Foreldrepenger, Pleiepenger, Omsorgspenger, Opplæringspenger, Institusjonsopphold, Arbeidsavklaringspenger, Dagpenger, Dødsinfo" -> "Ytelser (uten sykepengehistorikk)"
            "Sykepengehistorikk, Foreldrepenger, Pleiepenger, Omsorgspenger, Opplæringspenger, Institusjonsopphold, Arbeidsavklaringspenger, Dagpenger, Dødsinfo" -> "Ytelser (med sykepengehistorikk)"
            "InntekterForSykepengegrunnlag, ArbeidsforholdV2, InntekterForSammenligningsgrunnlag, Medlemskap" -> "Vilkårsgrunnlag"
            else -> behov
        }

        internal fun erBarnAv(otherId: String, depth: Int): Boolean {
            if (depth < 0) return false
            if (this.id == otherId) return true
            if (matchAgainstSaksbehandlerløsningOrOppgave(otherId)) return true // weirdness for matching Godkjenning-solution against saksbehandler_løsning
            return children.any { it.erBarnAv(otherId, depth - 1) }
        }

        internal fun leggTil(parentId: String, message: Message): Boolean {
            if (this.id == parentId) return add(message)
            if (children.reversed().any { it.leggTil(parentId, message) }) {
                message.parents.add(0, this)
                return true
            }
            if (matchAgainstSaksbehandlerløsningOrOppgave(message.id)) return add(message) // weirdness for matching Godkjenning-solution against saksbehandler_løsning
            if (message.matchAgainstSaksbehandlerløsningOrOppgave(this.id)) return add(message) // weirdness for matching saksbehandler_løsning against Godkjenning-need
            return false
        }

        private fun add(other: Message): Boolean {
            other.parents.add(this)
            return children.add(other)
        }

        private fun matchAgainstSaksbehandlerløsningOrOppgave(id: String) =
            this.eventName in setOf("oppgave_opprettet", "saksbehandler_løsning") && id == this.node.path("hendelseId").asText()

        private fun decode(): String {
            val sb = StringBuilder()
            if (node.hasNonNull("vedtaksperiodeId")) {
                sb.append("vedtaksperiodeId: ")
                sb.append(node.path("vedtaksperiodeId").asText())
                sb.append(" ")
            }

            if (node.hasNonNull("utbetalingId")) {
                sb.append("utbetalingId: ")
                sb.append(node.path("utbetalingId").asText())
                sb.append(" ")
            }
            when (eventName) {
                "vedtaksperiode_endret" -> sb.append(node.path("forrigeTilstand").asText()).append(" -> ").append(node.path("gjeldendeTilstand").asText())
                "utbetaling_endret" -> sb.append(node.path("forrigeStatus").asText()).append(" -> ").append(node.path("gjeldendeStatus").asText())
                "Godkjenning" -> node.path("@løsning").path("Godkjenning").takeUnless { it.isMissingNode || it.isNull }?.let { løsning ->
                    sb.append("godkjent: ")
                    sb.append(løsning.path("godkjent").asBoolean())
                    if (løsning.path("automatiskBehandling").asBoolean()) sb.append(" (automatisk")
                }
                "Utbetaling" -> node.path("@løsning").path("Utbetaling").takeUnless { it.isMissingNode || it.isNull }?.let { løsning ->
                    sb.append(løsning.path("status").asText())
                }
                else -> {}
            }
            return sb.toString()
        }

        override fun toString() = toString(depth, parents.firstOrNull()?.produced)

        private fun toString(depth: Int, rootTimestamp: LocalDateTime?): String {
            val diff = rootTimestamp?.let { Duration.between(rootTimestamp, produced) }?.let { duration ->
                "${duration.toMinutes()} min(s) ${duration.toSecondsPart()} sec(s) "
            } ?: ""
            return "  ".repeat(depth) + "> $diff$eventName$extra (↧ $depth): $id (partition ${record.partition()}, offset ${record.offset()}) ${decode()}${children.joinToString(separator = "") { "\n${it.toString(depth + 1, rootTimestamp ?: produced) }" }}"
        }
    }

    override fun verify(factory: ConsumerProducerFactory) {}
}
