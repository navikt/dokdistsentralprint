package no.nav.dokdistsentralprint.itest.config;

import no.nav.dokdistsentralprint.config.alias.DokdistmellomlagerProperties;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.config.azure.AzureTokenProperties;
import no.nav.dokdistsentralprint.storage.BucketStorage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.resilience.annotation.EnableResilientMethods;

import static org.mockito.Mockito.mock;


@SpringBootApplication(scanBasePackages = "no.nav.dokdistsentralprint")
@EnableResilientMethods
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		DokdistmellomlagerProperties.class,
		AzureTokenProperties.class,
		DokdistsentralprintProperties.class
})
@Import({
		JmsItestConfig.class,
		CacheManagerTest.class
})
@Profile("itest")
public class ApplicationTestConfig {

	@Bean
	public BucketStorage bucketStorage() {
		return mock(BucketStorage.class);
	}
}
