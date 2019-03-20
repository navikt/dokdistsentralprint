package no.nav.dokdistsentralprint.qdist009;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static org.apache.camel.LoggingLevel.ERROR;

import no.nav.dokdistsentralprint.exception.functional.AbstractDokdistsentralprintFunctionalException;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist009Route extends SpringRouteBuilder {

	public static final String SERVICE_ID = "qdist009";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";
	private static final String SFTP_FILETYPE = ".zip";
	private static final String SFTP_FILE_CONFIG = "binary=true&fileName=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}" + SFTP_FILETYPE + "&jschLoggingLevel=DEBUG";
	private static final String SFTP_SECURITY_CONFIG = "&privateKeyFile={{sftp.privateKeyFile}}&privateKeyPassphrase={{sftp.privateKeyPassphrase}}&useUserKnownHostsFile=false&preferredAuthentications=publickey";
	private static final String SFTP_SERVER = "sftp://srvdokdistsentralp@{{sftp.url}}:{{sftp.port}}/{{sftp.remoteFilePath}}?" + SFTP_FILE_CONFIG + SFTP_SECURITY_CONFIG;

	private final Qdist009Service qdist009Service;
	private final DistribuerForsendelseTilSentralPrintValidatorAndMapper distribuerForsendelseTilSentralPrintValidatorAndMapper;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist009;
	private final Queue qdist009FunksjonellFeil;

	@Inject
	public Qdist009Route(Qdist009Service qdist009Service,
						 DistribuerForsendelseTilSentralPrintValidatorAndMapper distribuerForsendelseTilSentralPrintValidatorAndMapper,
						 DokdistStatusUpdater dokdistStatusUpdater,
						 Queue qdist009,
						 Queue qdist009FunksjonellFeil) {
		this.qdist009Service = qdist009Service;
		this.distribuerForsendelseTilSentralPrintValidatorAndMapper = distribuerForsendelseTilSentralPrintValidatorAndMapper;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist009 = qdist009;
		this.qdist009FunksjonellFeil = qdist009FunksjonellFeil;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(true)
				.loggingLevel(ERROR));

		onException(AbstractDokdistsentralprintFunctionalException.class, JAXBException.class)
				.handled(true)
				.useOriginalMessage()
				.log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist009FunksjonellFeil.getQueueName());

		from("jms:" + qdist009.getQueueName() +
				"?transacted=true")
				.routeId(SERVICE_ID)
				.setExchangePattern(ExchangePattern.InOnly)
				.doTry()
				.setProperty(PROPERTY_BESTILLINGS_ID, simple("${in.header.callId}", String.class))
				.setProperty(PROPERTY_FORSENDELSE_ID, xpath("//forsendelseId/text()", String.class))
				.log(LoggingLevel.INFO, log, "qdist009 har mottatt forsendelse med " + getIdsForLogging())
				.process(exchange -> MDC.put(CALL_ID, (String) exchange.getProperty(PROPERTY_BESTILLINGS_ID)))
				.doCatch(Exception.class)
				.end()
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelseTilSentralPrint.class)))
				.bean(distribuerForsendelseTilSentralPrintValidatorAndMapper)
				.bean(qdist009Service)
				.to(SFTP_SERVER)
				.log(LoggingLevel.INFO, log, "qdist009 har lagt forsendelse med " + getIdsForLogging() + " på filshare til SITS for distribusjon via PRINT")
				.bean(dokdistStatusUpdater)
				.log(LoggingLevel.INFO, log, "qdist009 har oppdatert forsendelseStatus i dokdist og avslutter behandling av forsendelse med " + getIdsForLogging());
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
