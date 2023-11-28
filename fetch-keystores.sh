#!/bin/zsh

KEYTSTORE_PROD_NAME=aiven-prod-keystore.p12
TRUSTORE_PROD_NAME=aiven-prod-truststore.jks
KEYTSTORE_DEV_NAME=aiven-dev-keystore.p12
TRUSTORE_DEV_NAME=aiven-dev-truststore.jks
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

alias k=kubectl
function kx() {
    k config use-context "$1"
}

function checkKeystore() {
  if ! keytool -list -v -keystore "$1" -storepass changeme -storetype PKCS12 > /dev/null;
  then
      echo Keystore is corrupt, or bad password
  else
      echo Keystore "$1" seem to be ok ...
  fi
}

function checkTruststore() {
  if ! keytool -list -v -keystore "$1" -storepass changeme > /dev/null;
  then
      echo Truststore is corrupt, or bad password
  else
  	echo Trustore "$1" seem to be ok ...
  fi
}

function getSecrets() {
    BROKERS=$(k get secret "$1" -n tbd -o jsonpath='{.data.KAFKA_BROKERS}' | base64 -D)
    echo "Brokers: $BROKERS"
    k get secret "$1" -n tbd -o jsonpath='{.data.client\.keystore\.p12}' | tee "$2.base64" | base64 -D > "$2"
    k get secret "$1" -n tbd -o jsonpath='{.data.client\.truststore\.jks}' | tee "$3.base64" | base64 -D > "$3"
    checkKeystore "$2"
    checkTruststore "$3"

    echo "Writing properties to $4"
    {
        echo "config = aiven"
        echo "aiven.brokers.url = $BROKERS"
        echo "aiven.truststore.path = $(pwd)/$3"
        echo "aiven.truststore.password = changeme"
        echo "aiven.keystore.path = $(pwd)/$2"
        echo "aiven.keystore.password = changeme"
    } > "$4"
}

if test "$#" -eq 0; then
    echo "Usage: $0 <prod secret name> [<dev secret name>]"
    echo "You must provide prod secret name."
    exit 1
fi

kx prod-gcp
getSecrets "$1" $KEYTSTORE_PROD_NAME $TRUSTORE_PROD_NAME "$SCRIPT_DIR/config/prod-aiven.properties"

if ! test -z "$2"; then
    kx dev-gcp
    getSecrets "$2" $KEYTSTORE_DEV_NAME $TRUSTORE_DEV_NAME "$SCRIPT_DIR/config/dev-aiven.properties"
fi
