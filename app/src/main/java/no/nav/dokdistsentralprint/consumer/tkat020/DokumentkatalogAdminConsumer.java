package no.nav.dokdistsentralprint.consumer.tkat020;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.exception.functional.Tkat020FunctionalException;
import no.nav.dokdistsentralprint.exception.technical.AbstractDokdistsentralprintTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.Tkat020TechnicalException;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.config.azure.AzureTokenProperties.CLIENT_REGISTRATION_DOKMET;
import static no.nav.dokdistsentralprint.config.azure.AzureTokenProperties.getOAuth2AuthorizeRequestForAzure;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.constants.NavHeaders.NAV_CALLID;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
class DokumentkatalogAdminConsumer implements DokumentkatalogAdmin {

	private final String dokumenttypeInfoV4Url;
	private final WebClient webClient;
	private final ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

	public DokumentkatalogAdminConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
										ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
										WebClient webClient) {
		this.dokumenttypeInfoV4Url = dokdistsentralprintProperties.getEndpoints().getDokmet().getUrl();
		this.webClient = webClient;
		this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
	}

	@Override
	@Cacheable(TKAT020_CACHE)
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DokumenttypeInfo getDokumenttypeInfo(final String dokumenttypeId) {

		DokumentTypeInfoToV4 result = webClient.get()
				.uri(dokumenttypeInfoV4Url + "/" + dokumenttypeId)
				.attributes(getOauth2AuthorizedClient())
				.headers(this::createHeaders)
				.retrieve()
				.bodyToMono(DokumentTypeInfoToV4.class)
				.doOnError(handleError(dokumenttypeId))
				.block();

		return mapResponse(result);
	}

	private DokumenttypeInfo mapResponse(final DokumentTypeInfoToV4 response) {
		if (response == null) {
			throw new Tkat020FunctionalException("dokkat. respons fra DokumenttypeInfo er null");
		}

		if (response.getDokumentProduksjonsInfo() == null || response.getDokumentProduksjonsInfo().getDistribusjonInfo() == null) {
			throw new Tkat020FunctionalException(format("dokkat.DokumentProduksjonsInfo eller dokkat.DokumentProduksjonsInfo.DistribusjonInfo er null på dokument med dokumenttypeId=%s. Ikke et utgående dokument? dokumentType=%s", response
					.getDokumenttypeId(), response.getDokumentType()));
		}

		return DokumenttypeInfo.builder()
				.konvoluttvinduType(response.getDokumentProduksjonsInfo().getDistribusjonInfo().getKonvoluttvinduType())
				.sentralPrintDokumentType(response.getDokumentProduksjonsInfo().getDistribusjonInfo()
						.getSentralPrintDokumentType())
				.tosidigprint(response.getDokumentProduksjonsInfo().getDistribusjonInfo().getTosidigPrint())
				.portoklasse(response.getDokumentProduksjonsInfo().getDistribusjonInfo().getPortoklasse())
				.build();
	}

	private Consumer<Throwable> handleError(String dokumenttypeId) {
		return error -> {
			if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
				throw new Tkat020FunctionalException(format("TKAT020 feilet med statusKode=%s. Fant ingen dokumenttypeInfo med dokumenttypeId=%s. Feilmelding=%s",
						response.getStatusCode(),
						dokumenttypeId,
						response.getResponseBodyAsString()),
						error);
			} else {
				throw new Tkat020TechnicalException(format("TKAT020 feilet teknisk for dokumenttypeId=%s med feilmelding=%s",
						dokumenttypeId,
						error.getMessage()),
						error);
			}
		};
	}

	private Consumer<Map<String, Object>> getOauth2AuthorizedClient() {
		Mono<OAuth2AuthorizedClient> clientMono = oAuth2AuthorizedClientManager.authorize(getOAuth2AuthorizeRequestForAzure(CLIENT_REGISTRATION_DOKMET));
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(clientMono.block());
	}

	private void createHeaders(HttpHeaders headers) {
		headers.setContentType(APPLICATION_JSON);
		headers.set(NAV_CALLID, MDC.get(CALL_ID));
	}

}
