package no.nav.dokdistsentralprint.sdist009;

import jakarta.xml.bind.JAXBContext;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.kvittering.StatusRapport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.cluster.ClusteredRoutePolicy;
import org.apache.camel.support.processor.validation.SchemaValidationException;
import org.springframework.stereotype.Component;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class Sdist009Route extends RouteBuilder {

	private static final String SERVICE_ID = "sdist009";
	public static final String MAILPIECE_FILE_NAME = "MailpieceFileName";
	private static final long ANTALL_SEKUNDER_MELLOM_POLL = SECONDS.toMillis(60);

	private static final String INBOUND_SFTP_FOLDER =
			"sftp://{{sftp.url}}:{{sftp.port}}/{{dokdistsentralprint.sdist009.inbound-file-path}}" +
					"?username={{dokdistsentralprint.sdist009.sftp-username}}" +
					"&privateKeyFile={{sftp.private-key-file}}" +
					"&privateKeyPassphrase={{sftp.private-key-passphrase}}" +
					"&preferredAuthentications=publickey" +
					"&include=^MP_RAPPORT_XML-.*\\.xml$" +
					"&delay=" + ANTALL_SEKUNDER_MELLOM_POLL +
					"&maxMessagesPerPoll=5" +
					"&binary=true" +
					"&move=ferdig" +
					"&moveFailed=feilet" +
					"&scheduler=spring&scheduler.cron={{dokdistsentralprint.sdist009.cron}}";

	private final DokdistsentralprintProperties.Sdist009Properties sdist009Properties;
	private final Sdist009Service sdist009Service;
	private final LeaderElectionClusterService leaderElectionClusterService;

	public Sdist009Route(DokdistsentralprintProperties dokdistsentralprintProperties,
						 Sdist009Service sdist009Service,
						 LeaderElectionClusterService leaderElectionClusterService) {
		this.sdist009Properties = dokdistsentralprintProperties.getSdist009();
		this.sdist009Service = sdist009Service;
		this.leaderElectionClusterService = leaderElectionClusterService;
	}

	@Override
	public void configure() throws Exception {
		//@formatter:off

		// Aktiverer SFTP-consumeren kun når denne poden er leader
		getContext().addService(leaderElectionClusterService, true, true);
		ClusteredRoutePolicy leaderPolicy = ClusteredRoutePolicy.forNamespace(leaderElectionClusterService, SERVICE_ID);

		onException(SchemaValidationException.class)
				.useOriginalMessage()
				.log(WARN, log, "XSD feilet validering med ${exception}");

		from(INBOUND_SFTP_FOLDER)
				.routeId(SERVICE_ID)
				.routePolicy(leaderPolicy)
				.autoStartup(sdist009Properties.isEnabled())
				.log(INFO, log, "Sdist009 har hentet fil med filnavn=${file:name} fra sftp-server")
				.setProperty(MAILPIECE_FILE_NAME, simple("${file:name}"))
				.to("validator:no/nav/dokdistsentralprint/kvittering/mailpiece.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(StatusRapport.class)))
				.bean(sdist009Service)
				.end();

		//@formatter:on
	}
}
