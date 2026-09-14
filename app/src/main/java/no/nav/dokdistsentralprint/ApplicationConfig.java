package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.config.alias.DokdistmellomlagerProperties;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.NaisProperties;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@EnableResilientMethods
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		DokdistmellomlagerProperties.class,
		NaisProperties.class,
		DokdistsentralprintProperties.class
})
@Configuration
public class ApplicationConfig {
}
