package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.constants.NavHeadersFilter;
import no.nav.dokdistsentralprint.exception.functional.DokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokdistsentralprintTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

import static no.nav.dokdistsentralprint.config.azure.AzureTokenProperties.CLIENT_REGISTRATION_DOKDISTADMIN;
import static no.nav.dokdistsentralprint.config.azure.AzureTokenProperties.getOAuth2AuthorizeRequestForAzure;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final WebClient webClient;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public AdministrerForsendelseConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
										  WebClient webClient,
										  ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
		this.webClient = webClient.mutate()
				.baseUrl(dokdistsentralprintProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	@Retryable(retryFor = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {

		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.attributes(getOAuth2AuthorizedClient())
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.doOnError(this::handleError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Override
	@Retryable(retryFor = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterForsendelseStatus(OppdaterForsendelseRequest oppdaterForsendelseRequest) {

		webClient.put()
				.uri("/oppdaterforsendelse")
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(oppdaterForsendelseRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();
	}

	@Override
	@Retryable(retryFor = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String hentPostdestinasjon(String landkode) {

		log.info("hentPostdestinasjon henter postdestinasjon for landkode={}", landkode);

		var postdestinasjon = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/hentpostdestinasjon/{landkode}")
						.build(landkode))
				.attributes(getOAuth2AuthorizedClient())
				.retrieve()
				.bodyToMono(HentPostdestinasjonResponse.class)
				.map(HentPostdestinasjonResponse::postdestinasjon)
				.doOnError(this::handleError)
				.block();

		log.info("hentPostdestinasjon har hentet postdestinasjon={} for landkode={}", postdestinasjon, landkode);

		return postdestinasjon;
	}

	@Override
	@Retryable(retryFor = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterPostadresse(OppdaterPostadresseRequest oppdaterPostadresseRequest) {

		log.info("oppdaterPostadresse skal oppdatere postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());

		webClient.put()
				.uri("/oppdaterpostadresse")
				.attributes(getOAuth2AuthorizedClient())
				.bodyValue(oppdaterPostadresseRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("oppdaterPostadresse har oppdatert postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new DokdistsentralprintFunctionalException(
					String.format("Kall mot rdist001 feilet funksjonelt med status=%s, feilmelding=%s",
							response.getStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new DokdistsentralprintTechnicalException(
					String.format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}

	private Consumer<Map<String, Object>> getOAuth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(getOAuth2AuthorizeRequestForAzure(CLIENT_REGISTRATION_DOKDISTADMIN));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}

}