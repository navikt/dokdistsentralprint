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
import reactor.core.publisher.Mono;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.POSTDESTINASJON_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasWebClientRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class AdministrerForsendelseConsumer {

	private final WebClient texasAuthorizedWebClient;
	private final DokdistsentralprintProperties.Endpoints dokdistadminEndpoint;

	public AdministrerForsendelseConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
										  WebClient texasAuthorizedWebClient,
										  HttpCodecsProperties httpCodecsProperties) {
		this.dokdistadminEndpoint = dokdistsentralprintProperties.getEndpoints();
		this.texasAuthorizedWebClient = texasAuthorizedWebClient.mutate()
				.baseUrl(dokdistadminEndpoint.getDokdistadmin().getUrl())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultRequest(spec ->
						spec.attribute(TARGET_SCOPE, dokdistadminEndpoint.getDokdistadmin().getScope()))
				.codecs(configurer ->
						configurer.defaultCodecs().maxInMemorySize((int) httpCodecsProperties.getMaxInMemorySize().toBytes()))
				.build();
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public Long finnForsendelse(String bestillingsId) {
		log.info("finnForsendelse henter forsendelse med bestillingsId={}", bestillingsId);

		Long forsendelseId = texasAuthorizedWebClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/finnforsendelse/bestillingsId/{bestillingsId}")
						.build(bestillingsId))
				.retrieve()
				.bodyToMono(FinnForsendelseResponse.class)
				.onErrorResume(e -> {
					if (e instanceof WebClientResponseException response && NOT_FOUND.equals(response.getStatusCode())) {
						log.warn("finnForsendelse fant ikke forsendelse med bestillingsId={}", bestillingsId);
						return Mono.empty();
					}
					return Mono.error(mapError(e));
				})
				.map(FinnForsendelseResponse::forsendelseId)
				.block();

		if (forsendelseId != null) {
			log.info("finnForsendelse har hentet forsendelse med forsendelseId={} og bestillingsId={}", forsendelseId, bestillingsId);
		}

		return forsendelseId;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public HentForsendelseResponse hentForsendelse(String forsendelseId) {
		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = texasAuthorizedWebClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.onErrorMap(this::mapError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void oppdaterForsendelseStatus(OppdaterForsendelseRequest oppdaterForsendelseRequest) {
		texasAuthorizedWebClient.put()
				.uri("/oppdaterforsendelse")
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

		var postdestinasjon = texasAuthorizedWebClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/hentpostdestinasjon/{landkode}")
						.build(landkode))
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

		texasAuthorizedWebClient.put()
				.uri("/oppdaterpostadresse")
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

		texasAuthorizedWebClient.put()
				.uri("/feilregistrerforsendelse")
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

		Long filInfoId = texasAuthorizedWebClient.put()
				.uri("/oppdaterfilinformasjon")
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