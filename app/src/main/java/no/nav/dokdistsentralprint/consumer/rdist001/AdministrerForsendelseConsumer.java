package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.constants.NavHeadersFilter;
import no.nav.dokdistsentralprint.exception.functional.DokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokdistsentralprintTechnicalException;
import org.springframework.boot.autoconfigure.http.codec.HttpCodecsProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.config.azure.AzureTokenProperties.CLIENT_REGISTRATION_DOKDISTADMIN;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.POSTDESTINASJON_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final WebClient webClient;

	public AdministrerForsendelseConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
										  WebClient webClient,
										  HttpCodecsProperties httpCodecsProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistsentralprintProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer ->
								configurer.defaultCodecs().maxInMemorySize((int) httpCodecsProperties.getMaxInMemorySize().toBytes()))
						.build())
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
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
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
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterForsendelseRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();
	}

	@Override
	@Cacheable(POSTDESTINASJON_CACHE)
	@Retryable(retryFor = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String hentPostdestinasjon(String landkode) {

		log.info("hentPostdestinasjon henter postdestinasjon for landkode={}", landkode);

		var postdestinasjon = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/hentpostdestinasjon/{landkode}")
						.build(landkode))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
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
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterPostadresseRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("oppdaterPostadresse har oppdatert postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());
	}

	@Retryable(retryFor = DokdistsentralprintTechnicalException.class,  backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		webClient.put()
				.uri("/feilregistrerforsendelse")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			throw new DokdistsentralprintFunctionalException(
					format("Kall mot rdist001 feilet funksjonelt med status=%s, feilmelding=%s",
							response.getStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new DokdistsentralprintTechnicalException(
					format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}

}