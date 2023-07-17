package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.config.alias.DokdistmellomlagerProperties;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.config.azure.AzureTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		DokdistmellomlagerProperties.class,
		AzureTokenProperties.class,
		DokdistsentralprintProperties.class
})
@Configuration
public class ApplicationConfig {
}
