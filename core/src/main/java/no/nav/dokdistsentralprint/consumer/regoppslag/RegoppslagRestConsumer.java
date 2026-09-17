package no.nav.dokdistsentralprint.consumer.regoppslag;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistsentralprint.exception.functional.RegoppslagHentAdresseFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagHentAdresseTechnicalException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistsentralprint.constants.NavHeaders.NAV_REASON_CODE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.StreamUtils.copyToString;

@Slf4j
@Component
public class RegoppslagRestConsumer {

	private static final String HENT_MOTTAKER_OG_ADRESSE_PATH = "/rest/hentMottakerOgAdresse";
	private static final String UKJENT_ADRESSE_REASON_CODE = "ukjent_adresse";

	private final RestClient texasAuthorizedRestClient;
	private final DokdistsentralprintProperties.Endpoints regoppslagEndpoint;

	public RegoppslagRestConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
								  RestClient texasAuthorizedRestClient) {
		this.regoppslagEndpoint = dokdistsentralprintProperties.getEndpoints();
		this.texasAuthorizedRestClient = texasAuthorizedRestClient.mutate()
				.baseUrl(regoppslagEndpoint.getRegoppslag().getUrl())
				.defaultRequest(spec ->
						spec.attribute(TARGET_SCOPE, regoppslagEndpoint.getRegoppslag().getScope()))
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(includes = RegoppslagHentAdresseTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public AdresseTo treg002HentAdresse(HentAdresseRequestTo request) {

		return texasAuthorizedRestClient.post()
				.uri(uriBuilder -> uriBuilder.path(HENT_MOTTAKER_OG_ADRESSE_PATH).build())
				.body(request)
				.exchange((_, response) -> {
					if (response.getStatusCode().isError()) {
						var reasonCode = response.getHeaders().getFirst(NAV_REASON_CODE);
						if (NOT_FOUND.isSameCodeAs(response.getStatusCode()) &&
								UKJENT_ADRESSE_REASON_CODE.equals(reasonCode)) {
							log.warn("Kall mot TREG002 feilet funksjonelt med statusKode={}, reasonCode={}, feilmelding={}",
									response.getStatusCode(), reasonCode, copyToString(response.getBody(), UTF_8));
							return null;
						}
						handleError(response);
					}
					return response.bodyTo(HentMottakerOgAdresseResponseTo.class).getAdresse();
				});
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = copyToString(response.getBody(), UTF_8);
		String feilmelding = ("Kall mot TREG002 feilet %s med status=%s, feilmelding=%s")
				.formatted(response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk", response.getStatusCode(), body);

		if (response.getStatusCode().is4xxClientError()) {
			throw new RegoppslagHentAdresseFunctionalException(feilmelding);
		}
		throw new RegoppslagHentAdresseTechnicalException(feilmelding);
	}
}
