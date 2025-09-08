# HAG CLI

OPS-verktøy for å håndtere vanlige use cases.

Dette er HAGs versjon av [Bømlo CLI](https://github.com/navikt/bomlo-cli), og inneholder kun små endringer fra originalen.
Det vil fremdeles finnes referanser til Bømlo her og der.

## Installere

### 1. Laste ned launcher-script:
```shell
curl -fsLo /usr/local/bin/rr https://github.com/navikt/hag-cli/releases/latest/download/rr && chmod +x /usr/local/bin/rr
```

### 2. Hente secrets og generere config
Verktøyet trenger credentials mot Kafka-clusteret for å kunne utføre kommandoer, til dette brukes sertifikater.

Skriptet som henter sertifikater krever navnet på en secret i miljøet du skal operere mot, prod-gcp eller dev-gcp.

Du må være logget på k8s-clusteret.

Finn navn på en aiven-secret fra **prod-gcp** eller **dev-gcp**:
```shell
kubectl get secret -n=helsearbeidsgiver | grep aiven-
```

Kjør kommandoen, enten mot prod-gcp:
```shell
./fetch-keystores.sh <name-of-prod-secret>
```

Eller mot dev-gcp:
```shell
./fetch-keystores.sh --dev <name-of-dev-secret>
```

Det lages automatisk en fil med navn for miljøet man går mot, enten `config/prod-aiven.properties` eller `config/dev-aiven.properties`.


### 3. Går du mot prod? Koble opp mot naisdevice-gateway `aiven-prod`

Selv om foregående kommandoer for å liste og hente secrets fungerer fint uten denne er selve CLI-ets kommandoer
avhengige av den. Mot dev-gcp er det ikke nødvendig å koble opp mot gateway.

### 4. Kjøre kommandoer

`rr` vil automatisk sjekke, og evt. laste ned, ny versjon av CLI-et.

```shell
rr config/prod-aiven.properties <en kommando>
```

## Oppdatere

### Sjekke om det er ny oppdatering

Avslutter med exitkode=10 om det er ny oppdatering
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties check_version

Self version: 1.20220303071437
Tag: 1.20220403071437
New version available!
Run with `--download` (and an optional filename) to automatically download the new binary
```

### Laste ned ny oppdatering (til stdout)

Avslutter med exitkode=10 om det er ny oppdatering
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties check_version --download

?3?R?!?r???F␊├/┘⎻⎺┤┼├≥/┤├␋┌/S▒°␊U├␋┌⎽.␌┌▒⎽⎽PK
...
```


### Laste ned ny oppdatering (til fil)

Avslutter med exitkode=10 om det er ny oppdatering
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties check_version --download app.jar
```



## Eksempler

### Printe ut partitions for consumer-grupper:

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties current_partitions helsearbeidsgiver-im-inntekt-v1,helsearbeidsgiver-im-brreg-v1
```

### Printe ut offsets for consumer-grupper:

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties current_offsets helsearbeidsgiver-im-inntekt-v1
```

### Printe ut offsets for alle partisjoner på et gitt tidspunkt:

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties offsets helsearbeidsgiver.rapid <localdatetime string>
```

`<localdatetime string>` er noe som `LocalDateTime` kan parse, eksempel: `2023-10-10T23:12:13`

### Printe ut flowrate for consumer-grupper:

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties flowrate helsearbeidsgiver-im-inntekt-v1,helsearbeidsgiver-im-brreg-v1

helsearbeidsgiver-im-inntekt-v1:   10 msgs/s [max:  228 msgs/s, avg:   23 msgs/s], helsearbeidsgiver-im-brreg-v1:   41 msgs/s [max:   66 msgs/s, avg:   25 msgs/s]
```

### Printe ut flowrate for topics:

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties topic_flowrate helsearbeidsgiver.rapid

helsearbeidsgiver-im-inntekt-v1:  160 msgs/s [max:  436 msgs/s, avg:   37 msgs/s]
```

### Hoppe over en melding / sette offsets manuelt:
1. Finn consumer group og topic name, de finner man lettest i appen sin prod.yml under kafka
2. Finn ut hvilken partisjon og hvilken offset det gjelder. De kan man finne i loggmeldinger under feltnavnene `x_rapids_record_offset` og `x_rapids_record_partition`
3. Hvis appen man skal deale med har en HPA, må man ta den ut av spill, ellers kan den skalere opp nye pods veldig fort som går i veien for endring av offsets.
   1. For å slette HPA: `kubectl delete hpa <appname>`
      1. eller man kan gjøre `kubectl edit hpa <appname>` og endre `scaleTargetRef` sin name til en annen app, da slipper man å re-runne github action, men må huske på å sette den tilbake når man skalerer opp
   2. For å ta ned aktuell app: `kubectl scale deploy <appname> --replicas 0` (husk hvor mange replicas/pods den kjørte med, til siste punkt i guiden)
   3. Sjekke at appen er nede: `kubectl get pods -l app=<appname>`
Etter dette kan man endre offsets.
4. Bruk kommandoen
    ```shell
    java -jar build/libs/app.jar \
    config/prod-aiven.properties set_offsets <consumer group> <topic name>
    ```
5. Scriptet går igjennom partisjon for partisjon, når man kommer til partisjonen som stod i feilmeldingen, så skriver man inn `offset + 1`
6. Når det er ferdig setter vi opp antall partisjoner igjen med: `kubectl scale deploy <appname> ——replicas <antall replicas>` og for at det skal få virkning så re-runner vi siste actionen på github som deployet noe (eller sette tilbake `scaleTargetRef` for HPA-en)

### Produce melding på topic:
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties produce helsearbeidsgiver.rapid <record key> <path to file.json>

====================================================
Record produced to partition #5 with offset 7230180
====================================================
```

### Slette en consumergruppe:
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties delete_consumer_group <consumer group> [optional topic name]

====================================================
Consumer group helsearbeidsgiver-im-inntekt-v1 deleted
====================================================
```


### Liste alle consumers av en topic:
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties consumers <topic name>

Consumers of topic helsearbeidsgiver.rapid
- helsearbeidsgiver-im-altinn-v1
- helsearbeidsgiver-im-brreg-v1
- helsearbeidsgiver-im-inntekt-v1
```

### Observere events på topic
```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties observe <topic name>

SAK_OPPRETT_REQUESTED              : 7
OPPGAVE_OPPRETT_REQUESTED          : 5
FORESPØRSEL_MOTTATT                : 1
FORESPØRSEL_LAGRET                 : 1
OPPGAVE_LAGRET                     : 1
SAK_OPPRETTET                      : 1
TILGANG_FORESPOERSEL_REQUESTED     : 1
```

### Måle antall events (og totalstørrelse) innenfor et tidsvindu

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties measure helsearbeidsgiver.rapid 2022-03-26T06:00:00

Consuming messages … [==================================================>] 100.0 %
behov                                      : 45539 messages, summing to 104 MB
subsumsjon                                 : 6824 messages, summing to 14 MB
sendt_søknad_nav                           : 938 messages, summing to 10 MB
sendt_søknad_arbeidsgiver                  : 741 messages, summing to 8 MB
vedtaksperiode_endret                      : 3859 messages, summing to 8 MB
pong                                       : 18375 messages, summing to 8 MB
vedtaksperiode_påminnet                    : 9190 messages, summing to 6 MB
infotrygdendring_uten_fnr                  : 6525 messages, summing to 6 MB
påminnelse                                 : 9196 messages, summing to 5 MB
utbetaling_utbetalt                        : 197 messages, summing to 3 MB
app_status                                 : 1050 messages, summing to 3 MB
utbetaling_endret                          : 967 messages, summing to 3 MB
ny_søknad                                  : 829 messages, summing to 2 MB
vedtaksperiode_tid_i_tilstand              : 3095 messages, summing to 2 MB
infotrygdendring                           : 4350 messages, summing to 1 MB
planlagt_påminnelse                        : 3859 messages, summing to 1 MB
vedtak_fattet                              : 929 messages, summing to 0 MB
transaksjon_status                         : 213 messages, summing to 0 MB
oppdrag_kvittering                         : 213 messages, summing to 0 MB
opprett_oppgave                            : 518 messages, summing to 0 MB
hendelse_ikke_håndtert                     : 403 messages, summing to 0 MB
```

### Trace en melding

Man kan se alle etterfølgende meldinger for en gitt melding ved å oppgi dens `@id`:

Parametere:
- dybde
- starttidspunkt - default søker den to timer tilbake

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties trace <topic> <@id>

Found message at partition=12, offset = 32551218
Whole topic read, exiting
> Godkjenning: 888e3cb5-dc22-4e31-994a-fc8e1ef80c54 (partition 12, offset 32551218 vedtaksperiodeId: f1673abb-b0d9-4c64-897b-bcbabe23ffb3 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5
	> utbetaling_endret: c6e1687d-1600-408e-81e9-27653ae7fa77 (partition 12, offset 32551299 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 IKKE_UTBETALT -> GODKJENT
	> utbetaling_endret: 9ddd2859-ba38-42bb-9635-63869e7fbddb (partition 12, offset 32551300 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 GODKJENT -> SENDT
	> vedtaksperiode_endret: 166980e5-ab23-42ae-87d7-5a232c0ac87d (partition 12, offset 32551301 vedtaksperiodeId: f1673abb-b0d9-4c64-897b-bcbabe23ffb3 AVVENTER_GODKJENNING -> TIL_UTBETALING
	> Utbetaling: 9b8610b4-2ffe-4ff0-b085-e98baf0c91a4 (partition 12, offset 32551302 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5
		> utbetaling_endret: fea5d772-d07f-47b1-a0ce-0f6712f6cea8 (partition 12, offset 32551312 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 SENDT -> OVERFØRT
		> utbetaling_endret: 5dc35304-a634-4337-993d-9366be607735 (partition 12, offset 32551313 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 OVERFØRT -> UTBETALT
		> utbetaling_utbetalt: 7ad36ca4-3536-40e5-923b-8a608d1231fa (partition 12, offset 32551314 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5
		> vedtaksperiode_endret: e564fa57-7937-4946-aa4b-3009f10d92f5 (partition 12, offset 32551315 vedtaksperiodeId: f1673abb-b0d9-4c64-897b-bcbabe23ffb3 TIL_UTBETALING -> AVSLUTTET
		> vedtak_fattet: c658ea11-45c5-462d-b433-3fa520452608 (partition 12, offset 32551316 vedtaksperiodeId: f1673abb-b0d9-4c64-897b-bcbabe23ffb3 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5
		> vedtaksperiode_endret: d6129000-023f-44ac-9f2e-1bf6162715c3 (partition 12, offset 32551317 vedtaksperiodeId: 7497b243-066a-4563-a3bf-5898640d22a0 AVVENTER_SØKNAD_UFERDIG_FORLENGELSE -> AVVENTER_SØKNAD_FERDIG_FORLENGELSE
	> Utbetaling: 9b8610b4-2ffe-4ff0-b085-e98baf0c91a4 (partition 12, offset 32551303 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 OVERFØRT
	> Utbetaling (FINAL): 9b8610b4-2ffe-4ff0-b085-e98baf0c91a4 (partition 12, offset 32551306 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 OVERFØRT
	> Utbetaling: 9b8610b4-2ffe-4ff0-b085-e98baf0c91a4 (partition 12, offset 32551308 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 AKSEPTERT
	> Utbetaling (FINAL): 9b8610b4-2ffe-4ff0-b085-e98baf0c91a4 (partition 12, offset 32551309 utbetalingId: bb1ca40d-0ed3-41ca-86ae-805d7f3191e5 AKSEPTERT
```

### Følge meldinger på en topic

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties follow_topic <topic>

#3, offset 7403511 - påminnelse:  --> {"@event_name":"påminnelse", …
#5, offset 7403512 - ping:  --> {"@event_name":"ping",…
…
```

### Følge meldinger for en person på en topic

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties follow <topic> <fnr>

#3, offset 7403511 - ny_søknad:  --> {"@event_name":"ny_søknad", …
#5, offset 7403512 - inntektsmelding:  --> {"@event_name":"inntektsmelding",…
…
```

### Følge meldinger av en type

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties follow_event <topic> <event_name>

#3, offset 7403511 - pong:  --> {"@event_name":"pong", …
#5, offset 7403512 - pong:  --> {"@event_name":"pong",…
…
```

### Følge meldinger av en type, fra et bestemt tidspunkt, og søke etter evt. tekst

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties consume <topic> <event_name> [<optional localdatetime timestamp>, [<optional search string>]]

#3, offset 7403511 - pong:  --> {"@event_name":"pong", …
#5, offset 7403512 - pong:  --> {"@event_name":"pong",…
…
```

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver.
