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

echo "Exporting appdynamics environment variables"
if test -f /var/run/secrets/nais.io/appdynamics/appdynamics.env;
then
    export $(cat /var/run/secrets/nais.io/appdynamics/appdynamics.env)
    echo "Appdynamics environment variables exported"
else
    echo "No such file or directory found at /var/run/secrets/nais.io/appdynamics/appdynamics.env"
fi
