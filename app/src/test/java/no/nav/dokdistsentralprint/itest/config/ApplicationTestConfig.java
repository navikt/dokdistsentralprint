package no.nav.dokdistsentralprint.itest.config;

import static org.mockito.Mockito.mock;

import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistsentralprint.storage.S3Storage;
import no.nav.dokdistsentralprint.storage.Storage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@Import(JmsItestConfig.class)
public class ApplicationTestConfig {

	@Bean
	public AmazonS3 s3() {
		return mock(AmazonS3.class);
	}

	@Bean
	public Storage storage(AmazonS3 s3) {
		return new S3Storage(s3);
	}

}

