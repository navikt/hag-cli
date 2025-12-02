package no.nav.helse.cli.dialog

import java.util.UUID
import no.nav.helse.cli.dialog.Inntektsmelding.Status
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr

val orgnr = Orgnr("214398982")

val sykmelding =
    Sykmelding(
        sykmeldingId = UUID.randomUUID(),
        orgnr = orgnr,
        foedselsdato = java.time.LocalDate.of(1990, 1, 1),
        fulltNavn = "OLA NORDMANN",
        sykmeldingsperioder =
            listOf(
                Sykmeldingsperiode(
                    fom = java.time.LocalDate.of(2023, 1, 1),
                    tom = java.time.LocalDate.of(2023, 1, 31)
                )
            )
    )

val sykepengesoeknad =
    Sykepengesoeknad(
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        soeknadId = UUID.randomUUID()
    )

val inntektsmeldingsforespoersel =
    Inntektsmeldingsforespoersel(
        forespoerselId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr
    )

val inntektsmeldingAvvist =
    Inntektsmelding(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        innsendingId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        status = Status.FEILET,
        kanal = Inntektsmelding.Kanal.HR_SYSTEM_API
    )
val inntektsmeldingGodkjent =
    Inntektsmelding(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        innsendingId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        status = Status.GODKJENT,
        kanal = Inntektsmelding.Kanal.HR_SYSTEM_API
    )
val utgaattInntektsmeldingForespoersel =
    UtgaattInntektsmeldingForespoersel(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr
    )
val inntektsmeldingsforespoerseloppdatert =
    Inntektsmeldingsforespoersel(
        forespoerselId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr
    )
val utgaattOppdatertForespoersel =
    UtgaattInntektsmeldingForespoersel(
        forespoerselId = inntektsmeldingsforespoerseloppdatert.forespoerselId,
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr
    )
