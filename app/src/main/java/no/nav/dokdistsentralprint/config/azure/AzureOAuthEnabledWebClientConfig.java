package no.nav.dokdistsentralprint.config.azure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

@Configuration
public class AzureOAuthEnabledWebClientConfig {

	@Bean
	WebClient webClient(
			ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager
	) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2exchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);

		var nettyHttpClient = HttpClient.create()
				.responseTimeout(Duration.of(20, ChronoUnit.SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.filter(oauth2exchangeFilterFunction)
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService
	) {
		ClientCredentialsReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
		var nettyHttpClient = HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, ChronoUnit.SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		WebClient webClientWithProxy =  WebClient.builder()
				.clientConnector(clientHttpConnector)
				.build();

		WebClientReactiveClientCredentialsTokenResponseClient client = new WebClientReactiveClientCredentialsTokenResponseClient();
		client.setWebClient(webClientWithProxy);

		authorizedClientProvider.setAccessTokenResponseClient(client);

		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		return authorizedClientManager;
	}

	@Bean
	ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService(ReactiveClientRegistrationRepository reactiveClientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(reactiveClientRegistrationRepository);
	}

	@Bean
	ReactiveClientRegistrationRepository clientRegistrationRepository(ClientRegistration clientRegistration) {
		return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
	}

	@Bean
	ClientRegistration clientRegistration(@Value("{dokmet_scope}") String dokmetScope, AzureTokenProperties azureTokenProperties) {
		return ClientRegistration.withRegistrationId(AzureTokenProperties.CLIENT_REGISTRATION_ID)
				.tokenUri(azureTokenProperties.azureOpenidConfigTokenEndpoint())
				.clientId(azureTokenProperties.clientId())
				.clientSecret(azureTokenProperties.clientSecret())
				.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
				.authorizationGrantType(CLIENT_CREDENTIALS)
				.scope(dokmetScope)
				.build();
	}
}
