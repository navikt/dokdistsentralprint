package no.nav.dokdistsentralprint.consumer.regoppslag;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.constants.NavHeadersFilter;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistsentralprint.exception.functional.RegoppslagHentAdresseFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagHentAdresseTechnicalException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static no.nav.dokdistsentralprint.constants.NavHeaders.NAV_REASON_CODE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasWebClientRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class RegoppslagRestConsumer {

	private static final String HENT_MOTTAKER_OG_ADRESSE_PATH = "/rest/hentMottakerOgAdresse";
	private static final String UKJENT_ADRESSE_REASON_CODE = "ukjent_adresse";

	private final WebClient texasAuthorizedWebClient;
	private final DokdistsentralprintProperties.Endpoints regoppslagEndpoint;

	public RegoppslagRestConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
								  WebClient texasAuthorizedWebClient) {
		this.regoppslagEndpoint = dokdistsentralprintProperties.getEndpoints();
		this.texasAuthorizedWebClient = texasAuthorizedWebClient.mutate()
				.baseUrl(regoppslagEndpoint.getRegoppslag().getUrl())
				.filter(new NavHeadersFilter())
				.defaultRequest(spec ->
						spec.attribute(TARGET_SCOPE, regoppslagEndpoint.getRegoppslag().getScope()))
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(includes = RegoppslagHentAdresseTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public AdresseTo treg002HentAdresse(HentAdresseRequestTo request) {

		return texasAuthorizedWebClient.post()
				.uri(uriBuilder -> uriBuilder.path(HENT_MOTTAKER_OG_ADRESSE_PATH).build())
				.bodyValue(request)
				.retrieve()
				.bodyToMono(HentMottakerOgAdresseResponseTo.class)
				.map(HentMottakerOgAdresseResponseTo::getAdresse)
				.onErrorResume(WebClientResponseException.class, this::handleUkjentAdresse)
				.doOnError(handleError())
				.block();
	}

	private Mono<AdresseTo> handleUkjentAdresse(WebClientResponseException e) {
		var reasonCode = e.getHeaders().getFirst(NAV_REASON_CODE);

		if (NOT_FOUND.equals(e.getStatusCode()) && UKJENT_ADRESSE_REASON_CODE.equals(reasonCode)) {
			log.warn("Kall mot TREG002 feilet funksjonelt med statusKode={}, reasonCode={}, feilmelding={}", e.getStatusCode(), reasonCode, e.getResponseBodyAsString());
			return Mono.empty();
		}
		return Mono.error(e);
	}

	private Consumer<Throwable> handleError() {
		return error -> {
			if (error instanceof WebClientResponseException e) {
				if (e.getStatusCode().is4xxClientError()) {
					throw new RegoppslagHentAdresseFunctionalException(
							"Kall mot TREG002 feilet funksjonelt med statusKode=%s, feilmelding=%s".formatted(e.getStatusCode(), e.getResponseBodyAsString()),
							e);
				} else {
					throw new RegoppslagHentAdresseTechnicalException(
							"Kall mot TREG002 feilet teknisk med statusKode=%s, feilmelding=%s".formatted(e.getStatusCode(), e.getResponseBodyAsString()),
							e);
				}
			} else {
				throw new RegoppslagHentAdresseTechnicalException(
						"Kall mot TREG002 feilet med ukjent teknisk feil. Feilmelding=%s".formatted(error.getMessage()),
						error);
			}
		};
	}
}
