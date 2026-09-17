package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasTokenConsumer;
import no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasRequestInterceptor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static java.time.Duration.ofSeconds;

@Configuration
public class CoreConfig {

	@Bean
	public RestClient restClient() {
		return RestClient.builder()
				.requestFactory(jdkClientHttpRequestFactory())
				.build();
	}

	@Bean
	public RestClient texasAuthorizedRestClient(NaisTexasTokenConsumer naisTexasTokenConsumer) {
		return RestClient.builder()
				.requestFactory(jdkClientHttpRequestFactory())
				.requestInterceptor(new NaisTexasRequestInterceptor(naisTexasTokenConsumer))
				.build();
	}

	private JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return ClientHttpRequestFactoryBuilder.jdk()
				.withCustomizer(jdkClientHttpRequestFactory ->
						jdkClientHttpRequestFactory.setReadTimeout(ofSeconds(20))
				)
				.build();
	}

}
