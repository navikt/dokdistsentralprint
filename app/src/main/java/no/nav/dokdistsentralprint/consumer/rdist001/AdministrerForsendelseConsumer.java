package no.nav.dokdistsentralprint.consumer.rdist001;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;

import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.exception.DokdistsentralprintFunctionalException;
import no.nav.dokdistsentralprint.exception.DokdistsentralprintTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class AdministrerForsendelseConsumer implements AdministrerForsendelse {

	private final String administrerforsendelseV1Url;
	private final RestTemplate restTemplate;

	@Inject
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

	@Retryable(include = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public HentForsendelseResponseTo hentForsendelse(final String forsendelseId) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders("")); //todo fix
			return restTemplate.exchange(this.administrerforsendelseV1Url + "/" + forsendelseId, HttpMethod.GET, entity, HentForsendelseResponseTo.class)
					.getBody();
		} catch (HttpClientErrorException e) {
			throw new DokdistsentralprintFunctionalException(String.format("Kall mot rdist001 feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DokdistsentralprintTechnicalException(String.format("Kall mot rdist001 feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	@Retryable(include = DokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId) {
		try {
			HttpEntity entity = new HttpEntity<>(createHeaders(bestillingsId));
			String uri = UriComponentsBuilder.fromHttpUrl(administrerforsendelseV1Url)
					.queryParam("forsendelseId", forsendelseId)
					.queryParam("forsendelseStatus", forsendelseStatus)
					.toUriString();
			restTemplate.exchange(uri, HttpMethod.PUT, entity, Object.class);
		} catch (HttpClientErrorException e) {
			throw new DokdistsentralprintFunctionalException(String.format("Kall mot rdist001 feilet funksjonelt med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DokdistsentralprintTechnicalException(String.format("Kall mot rdist001 feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders(String bestillingsId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(CALL_ID, bestillingsId);
		return headers;
	}

}
