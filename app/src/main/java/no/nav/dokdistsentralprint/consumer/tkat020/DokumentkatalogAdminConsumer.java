package no.nav.dokdistsentralprint.consumer.tkat020;

import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.exception.technical.AbstractDokdistsentralprintTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.Tkat020TechnicalException;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Slf4j
@Component
class DokumentkatalogAdminConsumer implements DokumentkatalogAdmin {

	private final String dokumenttypeInfoV4Url;
	private final RestTemplate restTemplate;

	@Inject
	public DokumentkatalogAdminConsumer(@Value("${DokumenttypeInfo_v4_url}") String dokumenttypeInfoV4Url,
										RestTemplateBuilder restTemplateBuilder,
										final ServiceuserAlias serviceuserAlias) {
		this.dokumenttypeInfoV4Url = dokumenttypeInfoV4Url;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Cacheable(TKAT020_CACHE)
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DokumenttypeInfoTo getDokumenttypeInfo(final String dokumenttypeId) {
		try {
			DokumentTypeInfoToV4 response = restTemplate.getForObject(this.dokumenttypeInfoV4Url + "/" + dokumenttypeId, DokumentTypeInfoToV4.class);
			return mapResponse(response);
		} catch (HttpClientErrorException e) {
			throw new Tkat020TechnicalException(String.format("TKAT020 feilet med statusKode=%s. Fant ingen dokumenttypeInfo med dokumenttypeId=%s. Feilmelding=%s", e
					.getStatusCode(), dokumenttypeId, e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new Tkat020TechnicalException(String.format("TKAT020 feilet teknisk med statusKode=%s, feilmelding=%s", e
					.getStatusCode(), e
					.getResponseBodyAsString()), e);
		}
	}

	//Todo: map de feltene vi trenger!
	private DokumenttypeInfoTo mapResponse(final DokumentTypeInfoToV4 response) {
		return DokumenttypeInfoTo.builder()
				.dokumentTittel(response.getDokumentTittel())
				.build();
	}

}
