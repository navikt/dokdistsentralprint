package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.constants.DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DISTRIBUSJON;
import static no.nav.dokdistsentralprint.constants.DomainConstants.HOVEDDOKUMENT;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.exception.functional.DokumentIkkeFunnetIS3Exception;
import no.nav.dokdistsentralprint.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;
import no.nav.dokdistsentralprint.storage.Storage;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist009Service {

	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final AdministrerForsendelse administrerForsendelse;
	private final Storage storage;

	@Inject
	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse, Storage storage) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
		this.storage = storage;
	}

	@Handler
	public File distribuerForsendelseTilSentralPrintService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo) {

		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(distribuerForsendelseTilSentralPrintTo.forsendelseId);
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(hentForsendelseResponseTo));
		List<DokdistDokument> dokdistDokumentList = getDocumentsFromS3(hentForsendelseResponseTo);

		//todo: kall regoppslag for adresse


		//todo: bygg bestillingsXml
		//todo: pakk forsendelse til zip-fil

		return new File("");
	}

	private void validateForsendelseStatus(String forsendelseStatus) {
		if (!FORSENDELSE_STATUS_KLAR_FOR_DISTRIBUSJON.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(format("ForsendelseStatus må være %s. Fant forsendelseStatus=%s", FORSENDELSE_STATUS_KLAR_FOR_DISTRIBUSJON, forsendelseStatus));
		}
	}

	private String getDokumenttypeIdHoveddokument(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(DokumentTo -> HOVEDDOKUMENT.equals(DokumentTo.getTilknyttetSom()))
				.map(HentForsendelseResponseTo.DokumentTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

	private List<DokdistDokument> getDocumentsFromS3(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.map(dokumentTo -> {
					String jsonPayload = storage.get(dokumentTo.getDokumentObjektReferanse())
							.orElseThrow(() -> new DokumentIkkeFunnetIS3Exception(format("Kunne ikke finne dokument i S3 med key=dokumentObjektReferanse=%s", dokumentTo
									.getDokumentObjektReferanse())));
					return JsonSerializer.deserialize(jsonPayload, DokdistDokument.class);
				})
				.collect(Collectors.toList());
	}

}
