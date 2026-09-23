package no.nav.dokdistsentralprint.consumer.dokmet;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.exception.functional.DokmetFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokmetTechnicalException;
import no.nav.dokmet.api.tkat020.DokumenttypeInfoTo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DokmetConsumer {

	private final RestClient restClient;

	public DokmetConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
						  RestClient restClient) {
		this.restClient = restClient.mutate()
				.baseUrl(dokdistsentralprintProperties.getEndpoints().getDokmetUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultStatusHandler(HttpStatusCode::isError, (_, res) -> handleError(res))
				.build();
	}

	@Cacheable(DOKMET_CACHE)
	@Retryable(includes = {DokmetTechnicalException.class, ResourceAccessException.class}, multiplier = MULTIPLIER_SHORT)
	public Distribusjonsinfo hentDistribusjonsinfo(final String dokumenttypeId) {
		DokumenttypeInfoTo dokumenttypeInfo = restClient.get()
				.uri(uriBuilder -> uriBuilder.path("/{dokumenttypeId}")
						.build(dokumenttypeId))
				.retrieve()
				.body(DokumenttypeInfoTo.class);
		return mapResponse(dokumenttypeInfo);
	}

	private Distribusjonsinfo mapResponse(final DokumenttypeInfoTo response) {
		if (manglerDistribusjonsinfo(response)) {
			return null;
		}

		return new Distribusjonsinfo(
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getPortoklasse(),
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getKonvoluttvinduType(),
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getSentralPrintDokumentType(),
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getTosidigPrint()
		);
	}

	private void handleError(ClientHttpResponse response) throws IOException {
		String body = StreamUtils.copyToString(response.getBody(), UTF_8);
		if (response.getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(format("Dokmet feilet funksjonelt med statuskode=%s. Feilmelding=%s",
					response.getStatusCode(), body));
		}
		throw new DokmetTechnicalException(format("Dokmet feilet teknisk med feilmelding=%s", body));
	}

	private boolean manglerDistribusjonsinfo(DokumenttypeInfoTo response) {
		return response == null ||
				response.getDokumentProduksjonsInfo() == null ||
				response.getDokumentProduksjonsInfo().getDistribusjonInfo() == null;
	}
}