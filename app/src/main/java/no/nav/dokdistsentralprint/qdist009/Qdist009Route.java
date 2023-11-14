package no.nav.dokdistsentralprint.qdist009;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import no.nav.dokdistsentralprint.exception.functional.AbstractDokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import no.nav.opprettoppgave.tjenestespesifikasjon.v1.xml.jaxb2.gen.OpprettOppgave;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class Qdist009Route extends RouteBuilder {

    private static final String BEHANDLE_MANGLENDE_ADRESSE = "BEHANDLE_MANGLENDE_ADRESSE";

    public static final String SERVICE_ID = "qdist009";
    static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
    static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";
    private static final String SFTP_SERVER = "sftp://{{sftp.url}}:{{sftp.port}}/{{sftp.remote-file-path}}" +
            "?username={{sftp.username}}" +
            "&password=" +
            "&binary=true" +
            "&fileName=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.zip" +
            "&tempFileName=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.tmp" +
            "&privateKeyFile={{sftp.private-key-file}}" +
            "&jschLoggingLevel=TRACE" +
            "&privateKeyPassphrase={{sftp.private-key-passphrase}}" +
            "&preferredAuthentications=publickey";

    private final Qdist009Service qdist009Service;
    private final PostadresseValidatorOgForsendelseFeilregistrerService postadresseService;
    private final DistribuerForsendelseTilSentralPrintMapper distribuerForsendelseTilSentralPrintMapper;

    private final DokdistStatusUpdater dokdistStatusUpdater;
    private final Queue qdist009;
    private final Queue qopp001;
    private final Queue qdist009FunksjonellFeil;

    public Qdist009Route(Qdist009Service qdist009Service,
                         DistribuerForsendelseTilSentralPrintMapper distribuerForsendelseTilSentralPrintMapper,
                         DokdistStatusUpdater dokdistStatusUpdater,
                         Queue qdist009,
                         Queue qopp001,
                         Queue qdist009FunksjonellFeil,
                         PostadresseValidatorOgForsendelseFeilregistrerService postadresseService) {
        this.qdist009Service = qdist009Service;
        this.distribuerForsendelseTilSentralPrintMapper = distribuerForsendelseTilSentralPrintMapper;
        this.dokdistStatusUpdater = dokdistStatusUpdater;
        this.qdist009 = qdist009;
        this.qopp001 = qopp001;
        this.qdist009FunksjonellFeil = qdist009FunksjonellFeil;
        this.postadresseService = postadresseService;
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
                .setExchangePattern(InOnly)
                .process(new IdsProcessor())
                .to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/out/distribuertilkanal.xsd")
                .unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
                .bean(distribuerForsendelseTilSentralPrintMapper)
                .bean(postadresseService)
                .choice()
                    .when(simple("${body.postadresse}").isNull())
                        .log(INFO, log, "forsendelse med " + getIdsForLogging() + " mangler postadresse og sender manglende postadresse oppgave til qopp001")
                        .process(exchange -> {
                            /**
                             * Forsendelser som mangler postadresse feilregistert og sendes meldingen til qopp001 kø
                             * for å opprette oppgave for videre saksbehandling.
                             **/
                            ArkivInformasjon arkivInformasjon = exchange.getIn().getBody(InternForsendelse.class).getArkivInformasjon();
                            OpprettOppgave opprettOppgave = new OpprettOppgave();
                            opprettOppgave.setOppgaveType(BEHANDLE_MANGLENDE_ADRESSE);
                            opprettOppgave.setArkivSystem(arkivInformasjon.getArkivSystem().name());
                            opprettOppgave.setArkivKode(arkivInformasjon.getArkivId());
                            exchange.getIn().setBody(opprettOppgave);
                        })
                        .marshal(new JaxbDataFormat(JAXBContext.newInstance(OpprettOppgave.class)))
                        .convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
                        .to("jms:" + qopp001.getQueueName())
                .otherwise()
                    .bean(qdist009Service)
                    .to(SFTP_SERVER)
                    .log(INFO, log, "qdist009 har lagt forsendelse med " + getIdsForLogging() + " på filshare til SITS for distribusjon via PRINT")
                    .bean(dokdistStatusUpdater)
                    .log(INFO, log, "qdist009 har oppdatert forsendelsestatus i dokdist og avslutter behandling av forsendelse med " + getIdsForLogging())
                .endChoice()
                .end();
    }

    public static String getIdsForLogging() {
        return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
                "forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
    }
}
