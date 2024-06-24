package no.nav.dokdistsentralprint.consumer.dokmet;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.constants.NavHeadersFilter;
import no.nav.dokdistsentralprint.exception.functional.DokmetFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.DokmetTechnicalException;
import no.nav.dokmet.api.tkat020.DokumenttypeInfoTo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DokmetConsumer {

	private final WebClient webClient;

	public DokmetConsumer(DokdistsentralprintProperties dokdistsentralprintProperties,
						  WebClient webClient) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistsentralprintProperties.getEndpoints().getDokmet().getUrl())
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Cacheable(DOKMET_CACHE)
	@Retryable(retryFor = DokmetTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public DokumenttypeInfo hentDokumenttypeInfo(final String dokumenttypeId) {
		log.info("hentDokumenttypeInfo henter dokumenttypeinfo med dokumenttypeId={}", dokumenttypeId);

		DokumenttypeInfoTo result = webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/{dokumenttypeId}")
						.build(dokumenttypeId))
				.retrieve()
				.bodyToMono(DokumenttypeInfoTo.class)
				.doOnError(handleError(dokumenttypeId))
				.block();

		var dokumenttypeinfo = mapResponse(result);
		log.info("hentDokumenttypeInfo har hentet dokumenttypeinfo med dokumenttypeId={}", dokumenttypeId);

		return dokumenttypeinfo;
	}

	private DokumenttypeInfo mapResponse(final DokumenttypeInfoTo response) {
		if (response == null) {
			throw new DokmetFunctionalException("DokumenttypeInfo er null");
		}

		if (response.getDokumentProduksjonsInfo() == null || response.getDokumentProduksjonsInfo().getDistribusjonInfo() == null) {
			throw new DokmetFunctionalException(format("DokumentProduksjonsInfo eller DistribusjonInfo er null på dokument med dokumenttypeId=%s og dokumenttype=%s. Ikke et utgående dokument?",
					response.getDokumenttypeId(), response.getDokumentType()));
		}

		return new DokumenttypeInfo(
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getPortoklasse(),
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getKonvoluttvinduType(),
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getSentralPrintDokumentType(),
				response.getDokumentProduksjonsInfo().getDistribusjonInfo().getTosidigPrint()
		);
	}

	private Consumer<Throwable> handleError(String dokumenttypeId) {
		return error -> {
			if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
				throw new DokmetFunctionalException(format("Dokmet feilet med statuskode=%s. Fant ingen dokumenttypeInfo med dokumenttypeId=%s. Feilmelding=%s",
						response.getStatusCode(),
						dokumenttypeId,
						response.getResponseBodyAsString()),
						error);
			} else {
				throw new DokmetTechnicalException(format("Dokmet feilet teknisk for dokumenttypeId=%s med feilmelding=%s",
						dokumenttypeId,
						error.getMessage()),
						error);
			}
		};
	}

}