package no.nav.dokdistsentralprint.sdist009;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import no.nav.dokdistsentralprint.kvittering.StatusRapport;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;

import static org.apache.camel.LoggingLevel.WARN;

@Component
public class Sdist009Route extends RouteBuilder {

	public static final String SERVICE_ID = "sdist009";

	private static final String INBOUND_SFTP_FOLDER =
			"sftp://{{sftp.url}}:{{sftp.port}}/{{sftp.inbound-file-path}}" +
					"?username={{sftp.username}}" +
					"&privateKeyFile={{sftp.private-key-file}}" +
					"&privateKeyPassphrase={{sftp.private-key-passphrase}}" +
					"&preferredAuthentications=publickey" +
					"&include=^MP_RAPPORT_XML-.*\\.xml$" +
					"&binary=true" +
					"&move=ferdig" +
					"&moveFailed=feilet" +
					"&scheduler=spring&scheduler.cron={{dokdistsentralprint.sdist009.cron}}";

	@Override
	public void configure() throws JAXBException {
		onException(SAXParseException.class)
				.useOriginalMessage()
				.log(WARN, log, "XSD feilet validering med ${exception}");

		from(INBOUND_SFTP_FOLDER)
				.routeId(SERVICE_ID)
				.autoStartup(false)
				.to("validator:no/nav/dokdistsentralprint/kvittering/mailpiece.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(StatusRapport.class)))
				.log("Har startet sdist009")
				.end();
	}
}
