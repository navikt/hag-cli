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


## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
