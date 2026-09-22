package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasRequestInterceptor;
import no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasTokenConsumer;
import no.nav.dokdistsentralprint.exception.technical.DokdistsentralprintTechnicalException;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ProxySelector;

import static java.time.Duration.ofSeconds;

@Configuration
public class CoreConfig {

	@Bean
	public RestClient restClient() {
		return RestClient.builder()
				.requestFactory(jdkClientHttpRequestFactory())
				.requestInterceptor(resourceAccessInterceptor())
				.build();
	}

	@Bean
	public RestClient texasAuthorizedRestClient(NaisTexasTokenConsumer naisTexasTokenConsumer) {
		return RestClient.builder()
				.requestFactory(jdkClientHttpRequestFactory())
				.requestInterceptor(
						new NaisTexasRequestInterceptor(naisTexasTokenConsumer))
				.build();
	}

	private JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return ClientHttpRequestFactoryBuilder.jdk()
				.withProxySelector(ProxySelector.getDefault())
				.withCustomizer(jdkClientHttpRequestFactory ->
						jdkClientHttpRequestFactory.setReadTimeout(ofSeconds(20))
				)
				.build();
	}

	private ClientHttpRequestInterceptor resourceAccessInterceptor() {
		return (request, body, execution) -> {
			try {
				return execution.execute(request, body);
			} catch (ResourceAccessException e) {
				throw new DokdistsentralprintTechnicalException("Ressursen er utilgjengelig. Feilmelding=%s".formatted(e.getMessage()), e);
			}
		};
	}
}
