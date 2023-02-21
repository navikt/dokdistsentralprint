package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.exception.functional.AbstractDokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.metrics.Qdist009MetricsRoutePolicy;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.ExchangePattern;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class Qdist009Route extends SpringRouteBuilder {

    public static final String SERVICE_ID = "qdist009";
    static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
    static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";
    private static final String SFTP_SERVER = "sftp://{{sftp.url}}:{{sftp.port}}/{{sftp.remoteFilePath}}" +
            "?username={{sftp.username}}" +
            "&password=" +
            "&binary=true" +
            "&fileName=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.zip" +
            "&tempFileName=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.tmp" +
            "&privateKeyFile={{sftp.privateKeyFile}}" +
            "&jschLoggingLevel=TRACE" +
            "&privateKeyPassphrase={{sftp.privateKeyPassphrase}}" +
            "&preferredAuthentications=publickey";

    private final Qdist009Service qdist009Service;
    private final DistribuerForsendelseTilSentralPrintMapper distribuerForsendelseTilSentralPrintMapper;

    private final DokdistStatusUpdater dokdistStatusUpdater;
    private final Queue qdist009;
    private final Queue qdist009FunksjonellFeil;
    private final Qdist009MetricsRoutePolicy qdist009MetricsRoutePolicy;

    public Qdist009Route(Qdist009Service qdist009Service,
                         DistribuerForsendelseTilSentralPrintMapper distribuerForsendelseTilSentralPrintMapper,
                         DokdistStatusUpdater dokdistStatusUpdater,
                         Queue qdist009,
                         Queue qdist009FunksjonellFeil,
                         Qdist009MetricsRoutePolicy qdist009MetricsRoutePolicy) {
        this.qdist009Service = qdist009Service;
        this.distribuerForsendelseTilSentralPrintMapper = distribuerForsendelseTilSentralPrintMapper;
        this.dokdistStatusUpdater = dokdistStatusUpdater;
        this.qdist009 = qdist009;
        this.qdist009FunksjonellFeil = qdist009FunksjonellFeil;
        this.qdist009MetricsRoutePolicy = qdist009MetricsRoutePolicy;
    }

    @Override
    public void configure() throws Exception {
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(0)
                .log(log)
                .logExhaustedMessageBody(false)
                .logStackTrace(true)
                .loggingLevel(ERROR));

        onException(AbstractDokdistsentralprintFunctionalException.class, JAXBException.class)
                .handled(true)
                .useOriginalMessage()
                .log(WARN, log, "${exception}; " + getIdsForLogging())
                .to("jms:" + qdist009FunksjonellFeil.getQueueName());

        from("jms:" + qdist009.getQueueName() + "?transacted=true&concurrentConsumers=2")
                .routeId(SERVICE_ID)
                .routePolicy(qdist009MetricsRoutePolicy)
                .setExchangePattern(ExchangePattern.InOnly)
                .process(new IdsProcessor())
                .to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/out/distribuertilkanal.xsd")
                .unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
                .bean(distribuerForsendelseTilSentralPrintMapper)
                .bean(qdist009Service)
                .to(SFTP_SERVER)
                .log(INFO, log, "qdist009 har lagt forsendelse med " + getIdsForLogging() + " på filshare til SITS for distribusjon via PRINT")
                .bean(dokdistStatusUpdater)
                .log(INFO, log, "qdist009 har oppdatert forsendelseStatus i dokdist og avslutter behandling av forsendelse med " + getIdsForLogging());
    }

    public static String getIdsForLogging() {
        return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
                "forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
    }
}
