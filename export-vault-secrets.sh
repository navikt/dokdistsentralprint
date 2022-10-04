#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdokdistsentralprint/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdokdistsentralprint/username)
fi

if test -f /var/run/secrets/nais.io/srvdokdistsentralprint/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdokdistsentralprint/password)
fi

if test -f /var/run/secrets/nais.io/privateKey/privateKeyFile;
then
    echo "Setting sftp_privateKeyFile"
    export sftp_privateKeyFile=/var/run/secrets/nais.io/privateKey/privateKeyFile
fi

if test -f /var/run/secrets/nais.io/privateKey/privateKeyPassphrase;
then
    echo "Setting sftp_privateKeyPassphrase"
    export sftp_privateKeyPassphrase=$(cat /var/run/secrets/nais.io/privateKey/privateKeyPassphrase)
fi

if test -f /var/run/secrets/nais.io/vault/gcloud_serviceaccount
then
    echo "Setting GOOGLE_APPLICATION_CREDENTIALS"
    export GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/nais.io/vault/gcloud_serviceaccount
fi

if test -f /var/run/secrets/nais.io/certificate/keystore
then
    echo "Setting DOKDISTSENTRALPRINTCERT_KEYSTORE"
    CERT_PATH='/var/run/secrets/nais.io/certificate/keystore-extracted'
    openssl base64 -d -A -in /var/run/secrets/nais.io/certificate/keystore -out $CERT_PATH
    export DOKDISTSENTRALPRINTCERT_KEYSTORE=$CERT_PATH
fi

if test -f /var/run/secrets/nais.io/certificate/keystorepassword
then
    echo "Setting DOKDISTSENTRALPRINTCERT_PASSWORD"
    export DOKDISTSENTRALPRINTCERT_PASSWORD=$(cat /var/run/secrets/nais.io/certificate/keystorepassword)
fi