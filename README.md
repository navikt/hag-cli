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

### Sette offsets manuelt:

```shell
% java -jar build/libs/app.jar \
  config/prod-aiven.properties set_offsets tbd-spleis-v1 tbd.rapid.v1
```


## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
