package no.nav.dokdistsentralprint.consumer.regoppslag;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistsentralprint.exception.functional.RegoppslagHentAdresseFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.AbstractDokdistsentralprintTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagHentAdresseSecurityException;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagHentAdresseTechnicalException;
import no.nav.dokdistsentralprint.qdist009.reststs.StsRestConsumer;
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

import java.time.Duration;
import java.util.UUID;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.constants.NavHeaders.NAV_CALLID;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
public class RegoppslagRestConsumer implements Regoppslag {

	private final RestTemplate restTemplate;
	private final String hentMottakerOgAdresseUrl;
	private final StsRestConsumer stsRestConsumer;

	public RegoppslagRestConsumer(RestTemplateBuilder restTemplateBuilder,
								  @Value("${regoppslag.hentmottakerogadresse.url}") String hentMottakerOgAdresseUrl,
								  StsRestConsumer stsRestConsumer) {
		this.hentMottakerOgAdresseUrl = hentMottakerOgAdresseUrl;
		this.stsRestConsumer = stsRestConsumer;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Override
	@Retryable(retryFor = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public AdresseTo treg002HentAdresse(HentAdresseRequestTo request) {
		HttpEntity<?> entity = new HttpEntity<>(request, retrieveBearerTokenAndCreateHeader());
		try {
			return restTemplate.postForObject(this.hentMottakerOgAdresseUrl, entity, HentMottakerOgAdresseResponseTo.class)
					.getAdresse();
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().equals(UNAUTHORIZED)) {
				throw new RegoppslagHentAdresseSecurityException(format("Kall mot TREG002 feilet. Ingen tilgang. Feilmelding=%s", e
						.getMessage()));
			}

			if (e.getStatusCode().equals(NOT_FOUND)) {
				log.warn(format("Kall mot TREG002 feilet funksjonelt. HttpStatusKode=%s, HttpRespons=%s, Feilmelding=%s", e
						.getStatusCode(), e.getResponseBodyAsString(), e.getMessage()));
				return null;
			}
			throw new RegoppslagHentAdresseFunctionalException(format("Kall mot TREG002 feilet funksjonelt. HttpStatusKode=%s, HttpRespons=%s, Feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString(), e.getMessage()));
		} catch (HttpServerErrorException e) {
			throw new RegoppslagHentAdresseTechnicalException(format("Kall mot TREG002 feilet teknisk. HttpStatusKode=%s, Feilmelding=%s", e
					.getStatusCode(), e.getMessage()));
		}
	}

	private HttpHeaders retrieveBearerTokenAndCreateHeader() {
		String callId = getCallId();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setBearerAuth(stsRestConsumer.getBearerToken());
		httpHeaders.set(CALL_ID, callId);
		httpHeaders.set(NAV_CALLID, callId);
		return httpHeaders;
	}

	private String getCallId() {
		String callId = MDC.get(CALL_ID);
		if (callId == null) {
			return UUID.randomUUID().toString();
		}
		return callId;
	}
}
