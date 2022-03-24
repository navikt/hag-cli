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

### Hente secrets

```shell
./fetch-keystores.sh name-of-prod-secret optional-name-of-dev-secret
```

Det lages automatisk en fil kalt `config/prod-aiven.properties` og `config/dev-aiven.properties`.

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
