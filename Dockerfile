FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics

COPY app/target/app.jar /app/app.jar
COPY export-vault-secrets.sh /init-scripts/50-export-vault-secrets.sh
COPY dokdistsentralprint-java-opts.sh /init-scripts/51-dokdistsentralprint-java-opts.sh
ENV APPD_ENABLED=true