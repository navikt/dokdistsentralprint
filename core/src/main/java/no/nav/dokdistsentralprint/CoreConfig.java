package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasTokenConsumer;
import no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasWebClientRequestInterceptor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;

@Configuration
public class CoreConfig {

	@Bean
	WebClient texasAuthorizedWebClient(NaisTexasTokenConsumer naisTexasTokenConsumer,
									   HttpClient httpClient) {
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.filter(new NaisTexasWebClientRequestInterceptor(naisTexasTokenConsumer))
				.build();
	}

	@Bean
	WebClient webClient(HttpClient httpClient) {
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	@Bean
	public RestClient restClient() {
		return RestClient.builder()
				.requestFactory(jdkClientHttpRequestFactory())
				.build();
	}

	private JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return ClientHttpRequestFactoryBuilder.jdk()
				.withCustomizer(jdkClientHttpRequestFactory ->
						jdkClientHttpRequestFactory.setReadTimeout(ofSeconds(20))
				)
				.build();
	}

	@Bean
	public HttpClient httpClient() {
		return HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, SECONDS));
	}
}
