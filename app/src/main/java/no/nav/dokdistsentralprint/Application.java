package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.config.alias.DokdistmellomlagerProperties;
import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

import static java.lang.System.getenv;
import static java.lang.System.setProperty;

@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		DokdistmellomlagerProperties.class})
public class Application {
	public static void main(String[] args) {
		setProperty("javax.net.ssl.keyStorePassword", getenv("DOKDISTSENTRALPRINTCERT_PASSWORD"));
		SpringApplication.run(Application.class, args);
	}
}
