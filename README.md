# Bømlo CLI

OPS-verktøy for å håndtere vanlige use cases.

## Installere

### 1. Laste ned launcher-script:
```shell
curl -fsLo /usr/local/bin/rr https://github.com/navikt/bomlo-cli/releases/latest/download/rr && chmod +x /usr/local/bin/rr
```

### 2. Hente secrets og generere config

Finn navn på en aiven-secret fra prod-gcp
```shell
kubectl get secret | grep aiven-
```
Gjenta eventuelt for dev-gcp.

Kjør kommandoen:
```shell
./fetch-keystores.sh <name-of-prod-secret> <optional-name-of-dev-secret>
```

Det lages automatisk en fil kalt `config/prod-aiven.properties` og `config/dev-aiven.properties`.

### 3. Kjøre

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
  config/prod-aiven.properties current_partitions tbd-spleis-v1,tbd-spesialist-v1
```

### Printe ut offsets for consumer-grupper:

```shell
java -jar build/libs/app.jar \
  config/prod-aiven.properties current_offsets tbd-spleis-v1
```

### Printe ut flowrate for consumer-grupper:

```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties flowrate tbd-spleis-v1,tbd-spesialist-v1

tbd-spesialist-v1:   10 msgs/s [max:  228 msgs/s, avg:   23 msgs/s], tbd-spleis-v1:   41 msgs/s [max:   66 msgs/s, avg:   25 msgs/s]
```

### Printe ut flowrate for topics:

```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties topic_flowrate tbd.rapid.v1

tbd-spleis-v1:  160 msgs/s [max:  436 msgs/s, avg:   37 msgs/s]
```

### Sette offsets manuelt:

```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties set_offsets tbd-spleis-v1 tbd.rapid.v1
```

### Produce melding på topic:
```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties produce tbd.rapid.v1 <record key> <path to file.json>

====================================================
Record produced to partition #5 with offset 7230180
====================================================
```

### Slette en consumergruppe:
```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties delete_consumer_group <consumer group> [optional topic name]

====================================================
Consumer group tbd-spangre-utsettelser-v1 deleted
====================================================
```


### Liste alle consumers av en topic:
```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties consumers <topic name>

Consumers of topic tbd.rapid.v1
- helserisk-treskeverk-consumer
- sigmund-consumer
- spedisjon-v1
- tbd-behovsakkumulator-v1
- tbd-spaghet-v1
- tbd-spammer-v1
- tbd-spare-v1
…
```

### Observere events på topic
```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties observe <topic name>

behov                                       : 1378
subsumsjon                                  : 679
vedtaksperiode_endret                       : 257
planlagt_påminnelse                         : 257
vedtaksperiode_tid_i_tilstand               : 210
infotrygdendring_uten_fnr                   : 165
pong                                        : 140
infotrygdendring                            : 110
påminnelse                                  : 108
vedtaksperiode_påminnet                     : 108
utbetaling_endret                           : 59
ny_søknad                                   : 54
oppgavestyring_opprett                      : 44
opprett_oppgave                             : 39
oppgavestyring_utsatt                       : 37
inntektsmelding                             : 37
sendt_søknad_nav                            : 27
vedtak_fattet                               : 25
vedtaksperiode_forkastet                    : 25
oppgavestyring_ferdigbehandlet              : 20
hendelse_ikke_håndtert                      : 16
oppgave_oppdatert                           : 14
vedtaksperiode_godkjent                     : 13
oppdrag_kvittering                          : 13
transaksjon_status                          : 13
utbetaling_utbetalt                         : 13
sendt_søknad_arbeidsgiver                   : 12
trenger_ikke_inntektsmelding                : 12
app_status                                  : 7
saksbehandler_løsning                       : 6
oppgave_opprettet                           : 5
ping                                        : 4
opprett_oppgave_for_speilsaksbehandlere     : 2
oppgavestyring_opprett_speilrelatert        : 2
oppgavestyring_kort_periode                 : 2
trenger_inntektsmelding                     : 2
publisert_behov_for_inntektsmelding         : 2
vedtaksperiode_avvist                       : 2
behov_uten_fullstendig_løsning              : 1
```

### Måle antall events (og totalstørrelse) innenfor et tidsvindu

````shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties measure tbd.rapid.v1 2022-03-26T06:00:00

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
````

### Trace en melding

Man kan se alle etterfølgende meldinger for en gitt melding ved å oppgi dens `@id`:

Parametere:
- dybde
- starttidspunkt - default søker den to timer tilbake

````shell
% java -jar build/libs/app.jar \
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
````

### Følge meldinger på en topic

````shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties follow_topic <topic>

#3, offset 7403511 - påminnelse:  --> {"@event_name":"påminnelse", …
#5, offset 7403512 - ping:  --> {"@event_name":"ping",…
…
````

### Følge meldinger for en person på en topic

````shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties follow <topic> <fnr>

#3, offset 7403511 - ny_søknad:  --> {"@event_name":"ny_søknad", …
#5, offset 7403512 - inntektsmelding:  --> {"@event_name":"inntektsmelding",…
…
````

### Følge meldinger av en type

````shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties follow <topic> <event_name>

#3, offset 7403511 - pong:  --> {"@event_name":"pong", …
#5, offset 7403512 - pong:  --> {"@event_name":"pong",…
…
````

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
