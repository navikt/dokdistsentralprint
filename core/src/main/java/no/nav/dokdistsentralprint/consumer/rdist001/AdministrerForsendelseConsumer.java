package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.exception.functional.DokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokdistsentralprintTechnicalException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.POSTDESTINASJON_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.StreamUtils.copyToString;

@Slf4j
@Component
public class AdministrerForsendelseConsumer {

	private final RestClient texasAuthorizedRestClient;
	private final DokdistsentralprintProperties.Endpoints dokdistadminEndpoint;

	public AdministrerForsendelseConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
										  RestClient texasAuthorizedRestClient) {
		this.dokdistadminEndpoint = dokdistsentralprintProperties.getEndpoints();
		this.texasAuthorizedRestClient = texasAuthorizedRestClient.mutate()
				.baseUrl(dokdistadminEndpoint.getDokdistadmin().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultRequest(spec ->
						spec.attribute(TARGET_SCOPE, dokdistadminEndpoint.getDokdistadmin().getScope()))
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public Long finnForsendelse(String bestillingsId) {
		log.info("finnForsendelse henter forsendelse med bestillingsId={}", bestillingsId);

		FinnForsendelseResponse finnForsendelse = texasAuthorizedRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/finnforsendelse/bestillingsId/{bestillingsId}")
						.build(bestillingsId))
				.exchange((request, response) -> {
					if (response.getStatusCode().isError()) {
						if (NOT_FOUND.isSameCodeAs(response.getStatusCode())) {
							log.warn("finnForsendelse fant ikke forsendelse med bestillingsId={}", bestillingsId);
							return null;
						}
						handleError(response);
					}
					return response.bodyTo(FinnForsendelseResponse.class);
				});

		if (finnForsendelse == null) {
			return null;
		}

		log.info("finnForsendelse har hentet forsendelse med forsendelseId={} og bestillingsId={}", finnForsendelse.forsendelseId(), bestillingsId);

		return finnForsendelse.forsendelseId();
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public HentForsendelseResponse hentForsendelse(String forsendelseId) {
		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = texasAuthorizedRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.retrieve()
				.body(HentForsendelseResponse.class);

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void oppdaterForsendelseStatus(OppdaterForsendelseRequest oppdaterForsendelseRequest) {
		texasAuthorizedRestClient.put()
				.uri("/oppdaterforsendelse")
				.body(oppdaterForsendelseRequest)
				.retrieve()
				.toBodilessEntity();
	}

	@Cacheable(POSTDESTINASJON_CACHE)
	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public String hentPostdestinasjon(String landkode) {
		log.info("hentPostdestinasjon henter postdestinasjon for landkode={}", landkode);

		var postdestinasjon = texasAuthorizedRestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/hentpostdestinasjon/{landkode}")
						.build(landkode))
				.retrieve()
				.body(HentPostdestinasjonResponse.class)
				.postdestinasjon();

		log.info("hentPostdestinasjon har hentet postdestinasjon={} for landkode={}", postdestinasjon, landkode);

		return postdestinasjon;
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void oppdaterPostadresse(OppdaterPostadresseRequest oppdaterPostadresseRequest) {
		log.info("oppdaterPostadresse skal oppdatere postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());

		texasAuthorizedRestClient.put()
				.uri("/oppdaterpostadresse")
				.body(oppdaterPostadresseRequest)
				.retrieve()
				.toBodilessEntity();

		log.info("oppdaterPostadresse har oppdatert postadresse på forsendelse med forsendelseId={}", oppdaterPostadresseRequest.getForsendelseId());
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		texasAuthorizedRestClient.put()
				.uri("/feilregistrerforsendelse")
				.body(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	@Retryable(includes = DokdistsentralprintTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public Long oppdaterFilinformasjon(OppdaterFilinformasjonRequest oppdaterFilinformasjonRequest) {
		loggOpprettingEllerOppdateringAvFilinformasjon(oppdaterFilinformasjonRequest);

		Long filInfoId = texasAuthorizedRestClient.put()
				.uri("/oppdaterfilinformasjon")
				.body(oppdaterFilinformasjonRequest)
				.retrieve()
				.body(OppdaterFilinformasjonResponse.class)
				.filInfoId();

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

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = copyToString(response.getBody(), UTF_8);
		String feilmelding = ("Kall mot dokdistadmin feilet %s med status=%s, feilmelding=%s")
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk", response.getStatusCode(), body);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DokdistsentralprintFunctionalException(feilmelding);
		}
		throw new DokdistsentralprintTechnicalException(feilmelding);
	}
}