package no.nav.dokdistsentralprint.consumer.regoppslag;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistsentralprint.consumer.sts.STSTokenRetriever;
import no.nav.dokdistsentralprint.exception.functional.RegoppslagHentAdresseFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagHentAdresseSecurityException;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagHentAdresseTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.StsRetriveTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Slf4j
@Component
public class RegoppslagRestConsumer implements Regoppslag {

	private final RestTemplate restTemplate;
	private final String hentMottakerOgAdresseUrl;
	private final STSTokenRetriever stsTokenRetriever;


	public RegoppslagRestConsumer(RestTemplateBuilder restTemplateBuilder,
								  @Value("${hentMottakerOgAdresse_url}") String hentMottakerOgAdresseUrl,
								  final ServiceuserAlias serviceuserAlias,
								  STSTokenRetriever stsTokenRetriever) {
		this.hentMottakerOgAdresseUrl = hentMottakerOgAdresseUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
		this.stsTokenRetriever = stsTokenRetriever;
	}

	@Override
	public AdresseTo treg002HentAdresse(HentAdresseRequestTo request) {
		HttpEntity entity = createRequestWithHeader(request, retrieveSamlTokenAndCreateHeader());
		try {
			return restTemplate.postForObject(this.hentMottakerOgAdresseUrl, entity, HentMottakerOgAdresseResponseTo.class)
					.getAdresse();
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
				throw new RegoppslagHentAdresseSecurityException(String.format("Kall mot TREG002 feilet. Ingen tilgang. Feilmelding=%s", e
						.getMessage()));
			}
			throw new RegoppslagHentAdresseFunctionalException(String.format("Kall mot TREG002 feilet funksjonelt. HttpStatusKode=%s, Feilmelding=%s", e
					.getStatusCode(), e.getMessage()));
		} catch (HttpServerErrorException e) {
			throw new RegoppslagHentAdresseTechnicalException(String.format("Kall mot TREG002 feilet teknisk. HttpStatusKode=%s, Feilmelding=%s", e
					.getStatusCode(), e.getMessage()));
		}
	}

	private HttpHeaders retrieveSamlTokenAndCreateHeader() {
		try {
			String samlAssertionToken = stsTokenRetriever.requestSecurityToken();
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.set(HttpHeaders.AUTHORIZATION, "SAML " + Base64.getEncoder()
					.encodeToString(samlAssertionToken.getBytes(StandardCharsets.UTF_8)));
			return httpHeaders;
		} catch (Exception e) {
			throw new StsRetriveTokenException(String.format("Henting av samltoken fra STS feilet. Feilmelding=%s", e.getMessage()));
		}
	}

	private HttpEntity createRequestWithHeader(Object request, HttpHeaders httpHeaders) {
		return new HttpEntity(request, httpHeaders);
	}
}
