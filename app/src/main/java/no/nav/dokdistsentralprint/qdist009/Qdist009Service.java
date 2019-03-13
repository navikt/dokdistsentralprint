package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.constants.DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST;
import static no.nav.dokdistsentralprint.constants.DomainConstants.HOVEDDOKUMENT;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.RegoppslagRestConsumer;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.exception.functional.DokumentIkkeFunnetIS3Exception;
import no.nav.dokdistsentralprint.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
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
	private final RegoppslagRestConsumer regoppslagRestConsumer;
	private final Storage storage;

	@Inject
	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse, Storage storage,
						   RegoppslagRestConsumer regoppslagRestConsumer) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
		this.regoppslagRestConsumer = regoppslagRestConsumer;
		this.storage = storage;
	}

	@Handler
	public File distribuerForsendelseTilSentralPrintService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo) {

		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(distribuerForsendelseTilSentralPrintTo.forsendelseId);
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		DokumenttypeInfoTo dokumenttypeInfoTo = dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(hentForsendelseResponseTo));
		List<DokdistDokument> dokdistDokumentList = getDocumentsFromS3(hentForsendelseResponseTo);

		AdresseTo adresseTo = getAddresseIfNotProvided(hentForsendelseResponseTo);

		//todo: bygg bestillingsXml
		Bestilling bestilling = bestillingUtil.createBestilling(hentForsendelseResponseTo, dokumenttypeInfoTo);

		//todo: pakk forsendelse til zip-fil

		return new File("");
	}

	private AdresseTo getAddresseIfNotProvided(HentForsendelseResponseTo hentForsendelseResponseTo) {
		if (hentForsendelseResponseTo.getPostadresse() == null) {
			return regoppslagRestConsumer.treg002HentAdresse(
					new HentAdresseRequestTo(hentForsendelseResponseTo.getMottaker().getMottakerId(),
							hentForsendelseResponseTo.getMottaker().getMottakerType()));
		} else {
			return null;
		}
	}

	private void validateForsendelseStatus(String forsendelseStatus) {
		if (!FORSENDELSE_STATUS_KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(format("ForsendelseStatus må være %s. Fant forsendelseStatus=%s", FORSENDELSE_STATUS_KLAR_FOR_DIST, forsendelseStatus));
		}
	}

	private String getDokumenttypeIdHoveddokument(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(dokumentTo -> HOVEDDOKUMENT.equals(dokumentTo.getTilknyttetSom()))
				.map(HentForsendelseResponseTo.DokumentTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

	private List<DokdistDokument> getDocumentsFromS3(HentForsendelseResponseTo hentForsendelseResponseTo) {
		/**
		 * Her er rekkefølgen viktig. HentForsendelseResponseTo.getDokumenter består av en ordnet liste av dokumenter i rekkefølgen HOVEDDOK, VEDLEGG1, VEDLEGG2, ...
		 * Denne rekkefølgen må bevares slik at bestillingen blir korrekt. Siden vi bruker List.java blir denne rekkefølgen ivaretatt
		 **/
		return hentForsendelseResponseTo.getDokumenter().stream()
				.map(dokumentTo -> {
					String jsonPayload = storage.get(dokumentTo.getDokumentObjektReferanse())
							.orElseThrow(() -> new DokumentIkkeFunnetIS3Exception(format("Kunne ikke finne dokument i S3 med key=dokumentObjektReferanse=%s", dokumentTo
									.getDokumentObjektReferanse())));
					return JsonSerializer.deserialize(jsonPayload, DokdistDokument.class); //todo catch deserialization exception
				})
				.collect(Collectors.toList());
	}

}
