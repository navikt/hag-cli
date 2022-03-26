# Bømlo CLI

OPS-verktøy for å håndtere vanlige use cases.

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

### Hente secrets

```shell
./fetch-keystores.sh name-of-prod-secret optional-name-of-dev-secret
```

Det lages automatisk en fil kalt `config/prod-aiven.properties` og `config/dev-aiven.properties`.

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
