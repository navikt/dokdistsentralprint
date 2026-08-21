package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.constants.NavHeadersFilter;
import no.nav.dokdistsentralprint.exception.functional.DokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokdistsentralprintTechnicalException;
import org.springframework.boot.http.codec.autoconfigure.HttpCodecsProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.config.azure.AzureTokenProperties.CLIENT_REGISTRATION_DOKDISTADMIN;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.POSTDESTINASJON_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class AdministrerForsendelseConsumer {

	private final WebClient webClient;

	public AdministrerForsendelseConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
										  WebClient webClient,
										  HttpCodecsProperties httpCodecsProperties) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistsentralprintProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.codecs(configurer ->
						configurer.defaultCodecs().maxInMemorySize((int) httpCodecsProperties.getMaxInMemorySize().toBytes()))
				.build();
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public Long finnForsendelse(String bestillingsId) {
		log.info("finnForsendelse henter forsendelse med bestillingsId={}", bestillingsId);

		Long forsendelseId = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/finnforsendelse/bestillingsId/{bestillingsId}")
						.build(bestillingsId))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.retrieve()
				.bodyToMono(FinnForsendelseResponse.class)
				.map(FinnForsendelseResponse::forsendelseId)
				.onErrorMap(this::mapError)
				.block();

		log.info("finnForsendelse har hentet forsendelse med forsendelseId={} og bestillingsId={}", forsendelseId, bestillingsId);

		return forsendelseId;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {
		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.onErrorMap(this::mapError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void oppdaterForsendelseStatus(OppdaterForsendelseRequest oppdaterForsendelseRequest) {
		webClient.put()
				.uri("/oppdaterforsendelse")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterForsendelseRequest)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();
	}

	@Cacheable(POSTDESTINASJON_CACHE)
	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
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
				.onErrorMap(this::mapError)
				.block();

		log.info("hentPostdestinasjon har hentet postdestinasjon={} for landkode={}", postdestinasjon, landkode);

		return postdestinasjon;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void oppdaterPostadresse(OppdaterPostadresseRequest oppdaterPostadresseRequest) {
		log.info("oppdaterPostadresse skal oppdatere postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());

		webClient.put()
				.uri("/oppdaterpostadresse")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterPostadresseRequest)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("oppdaterPostadresse har oppdatert postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		webClient.put()
				.uri("/feilregistrerforsendelse")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public Long oppdaterFilinformasjon(OppdaterFilinformasjonRequest oppdaterFilinformasjonRequest) {
		loggOpprettingEllerOppdateringAvFilinformasjon(oppdaterFilinformasjonRequest);

		Long filInfoId = webClient.put()
				.uri("/oppdaterfilinformasjon")
				.attributes(clientRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN))
				.bodyValue(oppdaterFilinformasjonRequest)
				.retrieve()
				.bodyToMono(OppdaterFilinformasjonResponse.class)
				.map(OppdaterFilinformasjonResponse::filInfoId)
				.onErrorMap(this::mapError)
				.block();

		log.info("oppdaterFilinformasjon har opprettet/oppdatert forsendelse med filInfoId={}", filInfoId);

		return filInfoId;
	}

	private void loggOpprettingEllerOppdateringAvFilinformasjon(OppdaterFilinformasjonRequest oppdaterFilinformasjonRequest) {
		if (oppdaterFilinformasjonRequest.filInfoId() == null) {
			log.info("oppdaterFilinformasjon skal opprette filinformasjon for fil med filnavn={}", oppdaterFilinformasjonRequest.filnavn());
		} else {
			log.info("oppdaterFilinformasjon skal oppdatere filinformasjon for fil med filInfoId={}", oppdaterFilinformasjonRequest.filInfoId());
		}
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			return new DokdistsentralprintFunctionalException(
					format("Kall mot rdist001 feilet funksjonelt med status=%s, feilmelding=%s",
							response.getStatusCode(),
							response.getMessage()),
					error);
		} else {
			return new DokdistsentralprintTechnicalException(
					format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}

}