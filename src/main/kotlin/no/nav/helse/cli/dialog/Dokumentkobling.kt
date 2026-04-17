@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helse.cli.dialog

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

@Serializable
sealed class Dokumentkobling

@Serializable
@SerialName("Sykepengesoeknad")
data class Sykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr
) : Dokumentkobling()

@Serializable
@SerialName("VedtaksperiodeSoeknadKobling")
data class VedtaksperiodeSoeknadKobling(
    val vedtaksperiodeId: UUID,
    val soeknadId: UUID
) : Dokumentkobling()

@Serializable
@SerialName("ForespoerselSendt")
data class ForespoerselSendt(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr
) : Dokumentkobling()

@Serializable
@SerialName("ForespoerselUtgaatt")
data class ForespoerselUtgaatt(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr
) : Dokumentkobling()

@Serializable
@SerialName("InntektsmeldingAvvist")
data class InntektsmeldingAvvist(
    val inntektsmeldingId: UUID,
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: Orgnr
) : Dokumentkobling()
