#dokdistsentralprint

* [Funksjonelle Krav](#1-funksjonelle-krav)
* [Distribusjon av tjenesten (deployment)](#2-distribusjon-av-tjenesten-deployment)
* [Utviklingsmiljø](#3-utviklingsmilj)
* [Drift og støtte](#4-drift-og-sttte)

## Funksjonelle krav
Dokdistsentralprint er en del av Doksys og distribuerer dokumenter til sentral print hos Skatteetaten.

For mer informasjon: [confluence](https://confluence.adeo.no/display/BOA/dokdistsentralprint)


## Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort av Jenkins:
[regoppslag CI / CD](https://dok-jenkins.adeo.no/job/dokdistsentralprint/job/master/)
Push/merge til masterbranch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.


## Utviklingsmiljø
### Forutsetninger
* Java 11
* Kubectl
* Maven


### Bygge app.jar og kjøre tester
`mvn clean package`/`mvn clean install`


## Drift og støtte
### Logging
Loggene til tjenesten kan leses på to måter:

### Kibana
For [dev-fss](https://logs.adeo.no/goto/71e1a9aac32289cdd6c05a605148b540)

For [prod-fss](https://logs.adeo.no/goto/9bc304d3d21a4bc778f46e7af0161106)

### Kubectl
For dev-fss:
```shell script
kubectl config use-context dev-fss
kubectl get pods -n q1 -l app=dokdistsentralprint
kubectl logs -f dokdistsentralprint-<POD-ID> -n teamdokumenthandtering -c dokdistsentralprint
```

For prod-fss:
```shell script
kubectl config use-context prod-fss
kubectl get pods -l app=dokdistsentralprint
kubectl logs -f dokdistsentralprint-<POD-ID> -n teamdokumenthandtering -c dokdistsentralprint
```


### Henvendelser
Spørsmål kan rettes til Team Dokumentløsninger på:
* [\#Team Dokumentløsninger](https://nav-it.slack.com/client/T5LNAMWNA/C6W9E5GPJ)


