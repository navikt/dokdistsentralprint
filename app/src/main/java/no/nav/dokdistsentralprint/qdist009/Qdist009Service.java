package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.qdist009.util.FileUtils.marshalBestillingToXmlString;
import static no.nav.dokdistsentralprint.qdist009.util.FileUtils.zipPrintbestillingToBytes;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009Utils.createBestillingEntities;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009Utils.getDokumenttypeIdHoveddokument;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009Utils.validateForsendelseStatus;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.Regoppslag;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.exception.functional.DokumentIkkeFunnetIS3Exception;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;
import no.nav.dokdistsentralprint.storage.Storage;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist009Service {

	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final AdministrerForsendelse administrerForsendelse;
	private final Regoppslag regoppslag;
	private final Storage storage;
	private final BestillingMapper bestillingMapper;

	@Inject
	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse,
						   Storage storage,
						   Regoppslag regoppslag, BestillingMapper bestillingMapper) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
		this.regoppslag = regoppslag;
		this.storage = storage;
		this.bestillingMapper = bestillingMapper;
	}

	@Handler
	public byte[] distribuerForsendelseTilSentralPrintService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo) {
		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(distribuerForsendelseTilSentralPrintTo.forsendelseId);
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		DokumenttypeInfoTo dokumenttypeInfoTo = dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(hentForsendelseResponseTo));
		Adresse adresse = getAdresse(hentForsendelseResponseTo);
		String postdestinasjon = administrerForsendelse.findPostDestinasjon(adresse.getLandkode());

		List<DokdistDokument> dokdistDokumentList = getDocumentsFromS3(hentForsendelseResponseTo);


		Bestilling bestilling = bestillingMapper.createBestilling(hentForsendelseResponseTo, dokumenttypeInfoTo, adresse, postdestinasjon);
		String bestillingXmlString = marshalBestillingToXmlString(bestilling);
		List<BestillingEntity> bestillingEntities = createBestillingEntities(hentForsendelseResponseTo.getBestillingsId(), bestillingXmlString, dokdistDokumentList);
		return zipPrintbestillingToBytes(bestillingEntities);
	}

	private Adresse getAdresse(HentForsendelseResponseTo hentForsendelseResponseTo) {
		final HentForsendelseResponseTo.PostadresseTo adresseDokdist = hentForsendelseResponseTo.getPostadresse();
		if (adresseDokdist == null) {
			final AdresseTo adresseRegoppslag = getAdresseFromRegoppslag(hentForsendelseResponseTo);
			return Adresse.builder()
					.adresselinje1(adresseRegoppslag.getAdresselinje1())
					.adresselinje2(adresseRegoppslag.getAdresselinje2())
					.adresselinje3(adresseRegoppslag.getAdresselinje3())
					.landkode(adresseRegoppslag.getLandkode())
					.postnummer(adresseRegoppslag.getPostnummer())
					.poststed(adresseRegoppslag.getPoststed())
					.build();
		} else {
			return Adresse.builder()
					.adresselinje1(adresseDokdist.getAdresselinje1())
					.adresselinje2(adresseDokdist.getAdresselinje2())
					.adresselinje3(adresseDokdist.getAdresselinje3())
					.landkode(adresseDokdist.getLandkode())
					.postnummer(adresseDokdist.getPostnummer())
					.poststed(adresseDokdist.getPoststed())
					.build();
		}
	}

	private AdresseTo getAdresseFromRegoppslag(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return regoppslag.treg002HentAdresse(HentAdresseRequestTo.builder()
				.identifikator(hentForsendelseResponseTo.getMottaker().getMottakerId())
				.type(hentForsendelseResponseTo.getMottaker().getMottakerType())
				.build());
	}

	/**
	 * Her er rekkefølgen viktig. HentForsendelseResponseTo.dokumenter består av en ordnet liste av dokumenter i rekkefølgen HOVEDDOK, VEDLEGG1, VEDLEGG2, ...
	 * Denne rekkefølgen må bevares slik at bestillingen blir korrekt. Siden vi bruker List.java blir denne rekkefølgen ivaretatt
	 **/
	private List<DokdistDokument> getDocumentsFromS3(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.map(dokumentTo -> {
					String jsonPayload = storage.get(dokumentTo.getDokumentObjektReferanse())
							.orElseThrow(() -> new DokumentIkkeFunnetIS3Exception(format("Kunne ikke finne dokument i S3 med key=dokumentObjektReferanse=%s", dokumentTo
									.getDokumentObjektReferanse())));
					DokdistDokument dokdistDokument = JsonSerializer.deserialize(jsonPayload, DokdistDokument.class); //todo catch deserialization exception
					dokdistDokument.setDokumentObjektReferanse(dokumentTo.getDokumentObjektReferanse());
					return dokdistDokument;
				})
				.collect(Collectors.toList());
	}


}
