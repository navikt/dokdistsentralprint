package no.nav.dokdistsentralprint.consumer.rdist001;

import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.exception.functional.Rdist001GetPostDestinasjonFunctionalException;
import no.nav.dokdistsentralprint.exception.functional.Rdist001HentForsendelseFunctionalException;
import no.nav.dokdistsentralprint.exception.functional.Rdist001OppdaterForsendelseStatusFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.AbstractDokdistsentralprintTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.Rdist001GetPostDestinasjonTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.Rdist001HentForsendelseTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.Rdist001OppdaterForsendelseStatusTechnicalException;
import no.nav.dokdistsentralprint.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final String administrerforsendelseV1Url;
	private final RestTemplate restTemplate;

	public AdministrerForsendelseConsumer(@Value("${administrerforsendelse.v1.url}") String administrerforsendelseV1Url,
										  RestTemplateBuilder restTemplateBuilder,
										  final ServiceuserAlias serviceuserAlias) {
		this.administrerforsendelseV1Url = administrerforsendelseV1Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Override
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "hentForsendelse"}, histogram = true)
	public HentForsendelseResponseTo hentForsendelse(final String forsendelseId) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders());
			return restTemplate.exchange(this.administrerforsendelseV1Url + "/" + forsendelseId, GET, entity, HentForsendelseResponseTo.class)
					.getBody();
		} catch (HttpClientErrorException e) {
			throw new Rdist001HentForsendelseFunctionalException(String.format("Kall mot rdist001 - hentForsendelse feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001HentForsendelseTechnicalException(String.format("Kall mot rdist001 - hentForsendelse feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "oppdaterForsendelseStatus"}, histogram = true)
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders());
			String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
					.queryParam("forsendelseId", forsendelseId)
					.queryParam("forsendelseStatus", forsendelseStatus)
					.toUriString();
			restTemplate.exchange(uri, PUT, entity, Object.class);
		} catch (HttpClientErrorException e) {
			throw new Rdist001OppdaterForsendelseStatusFunctionalException(String.format("Kall mot rdist001 - oppdaterForsendelseStatus feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001OppdaterForsendelseStatusTechnicalException(String.format("Kall mot rdist001 - oppdaterForsendelseStatus feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "findPostDestinasjon"}, histogram = true)
	public HentPostDestinasjonResponseTo hentPostDestinasjon(String landkode) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders());
			return restTemplate.exchange(administrerforsendelseV1Url + "/hentpostdestinasjon/" + landkode, GET, entity, HentPostDestinasjonResponseTo.class)
					.getBody();
		} catch (HttpClientErrorException e) {
			throw new Rdist001GetPostDestinasjonFunctionalException(String.format("Kall mot rdist001 - GetPostDestinasjon feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001GetPostDestinasjonTechnicalException(String.format("Kall mot rdist001 - GetPostDestinasjon feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	@Override
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Monitor(value = "dok_consumer", extraTags = {"process", "oppdaterPostadresse"}, histogram = true)
	public void oppdaterPostadresse(OppdaterPostadresseRequest postadresse) {
		try {
			HttpEntity<?> entity = new HttpEntity<>(postadresse, createHeaders());
			restTemplate.exchange(administrerforsendelseV1Url + "/oppdaterpostadresse", PUT, entity, String.class);
		} catch (HttpClientErrorException e) {
			throw new Rdist001GetPostDestinasjonFunctionalException(String.format("Kall mot rdist001 - oppdaterPostadresse feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new Rdist001GetPostDestinasjonTechnicalException(String.format("Kall mot rdist001 - oppdaterPostadresse feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.set(CALL_ID, MDC.get(CALL_ID));
		return headers;
	}

}
