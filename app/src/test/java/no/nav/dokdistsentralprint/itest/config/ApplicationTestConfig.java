package no.nav.dokdistsentralprint.itest.config;

import no.nav.dokdistsentralprint.storage.BucketStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("itest")
@Import(JmsItestConfig.class)
public class ApplicationTestConfig {

	@Bean
	public BucketStorage bucketStorage() {
		return mock(BucketStorage.class);
	}
}

