package no.nav.dokdistsentralprint.consumer.dokmet;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.constants.NavHeaders;
import no.nav.dokdistsentralprint.exception.functional.DokmetFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokmetTechnicalException;
import no.nav.dokmet.api.tkat020.DokumenttypeInfoTo;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class DokmetConsumer {

	private final RestClient restClient;

	public DokmetConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
						  RestClient restClient) {
		this.restClient = restClient.mutate()
				.baseUrl(dokdistsentralprintProperties.getEndpoints().getDokmetUrl())
				.defaultHeaders(headers -> {
					headers.set(NavHeaders.NAV_CALLID, MDC.get(CALL_ID));
					headers.setContentType(APPLICATION_JSON);
				})
				.build();
	}

	@Cacheable(DOKMET_CACHE)
	@Retryable(includes = DokmetTechnicalException.class, multiplier = MULTIPLIER_SHORT)
	public Distribusjonsinfo hentDistribusjonsinfo(final String dokumenttypeId) {
		try {
			DokumenttypeInfoTo response = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/{dokumenttypeId}")
							.build(dokumenttypeId))
					.retrieve()
					.body(DokumenttypeInfoTo.class);
			return mapResponse(response);
		} catch (HttpClientErrorException e) {
			throw new DokmetFunctionalException(format("Dokmet feilet funksjonelt for dokumenttypeId=%s med statuskode=%s, feilmelding=%s",
					dokumenttypeId, e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DokmetTechnicalException(format("Dokmet feilet teknisk for dokumenttypeId=%s med feilmelding=%s",
					dokumenttypeId, e.getMessage()), e);
		}
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

	private boolean manglerDistribusjonsinfo(DokumenttypeInfoTo response) {
		return response == null ||
				response.getDokumentProduksjonsInfo() == null ||
				response.getDokumentProduksjonsInfo().getDistribusjonInfo() == null;
	}
}