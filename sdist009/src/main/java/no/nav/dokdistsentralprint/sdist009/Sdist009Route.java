package no.nav.dokdistsentralprint.sdist009;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

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
					"&scheduler=spring&scheduler.cron={{dokdistsentralprint.sdist009.cron}}";


	@Override
	public void configure() {
		from(INBOUND_SFTP_FOLDER)
				.routeId(SERVICE_ID)
				.autoStartup(false)
				.log("Har startet sdist009")
				.end();
	}
}
