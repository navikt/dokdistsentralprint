package no.nav.dokdistsentralprint;

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
	WebClient webClient() {
		var nettyHttpClient = HttpClient.create()
				.responseTimeout(Duration.of(20, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		return WebClient.builder()
				.clientConnector(clientHttpConnector)
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
}
