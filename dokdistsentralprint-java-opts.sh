#!/usr/bin/env sh

JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStore=${NAV_TRUSTSTORE_PATH}"
JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStorePassword=${NAV_TRUSTSTORE_PASSWORD}"
JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStoreType=jks"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxRAMPercentage=75"
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"
JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=nais"

export JAVA_OPTS
