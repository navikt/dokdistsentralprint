package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.config.alias.DokdistmellomlagerProperties;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.config.azure.AzureTokenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		DokdistmellomlagerProperties.class,
		AzureTokenProperties.class,
		DokdistsentralprintProperties.class
})
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
