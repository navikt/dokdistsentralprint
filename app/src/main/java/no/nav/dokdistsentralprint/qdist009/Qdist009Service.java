package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.rdist001.HentPostDestinasjonResponseTo;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterPostadresseRequest;
import no.nav.dokdistsentralprint.consumer.regoppslag.Regoppslag;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.exception.functional.KunneIkkeDeserialisereBucketJsonPayloadFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.NoDocumentFromBucketTechnicalException;
import no.nav.dokdistsentralprint.metrics.MetricUpdater;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.qdist009.domain.Adresse;
import no.nav.dokdistsentralprint.qdist009.domain.BestillingEntity;
import no.nav.dokdistsentralprint.qdist009.domain.DistribuerForsendelseTilSentralPrintTo;
import no.nav.dokdistsentralprint.storage.BucketStorage;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.createBestillingEntities;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.getDokumenttypeIdHoveddokument;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.validateForsendelseStatus;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009TechnicalUtils.marshalBestillingToXmlString;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009TechnicalUtils.zipPrintbestillingToBytes;
import static org.apache.commons.lang3.StringUtils.isBlank;


@Slf4j
@Service
public class Qdist009Service {

	private static final String UKJENT_LANDKODE = "???";
	private static final String XX_LANDKODE = "XX";
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final AdministrerForsendelse administrerForsendelse;
	private final Regoppslag regoppslag;
	private final BucketStorage bucketStorage;
	private final MetricUpdater metricUpdater;
	private final BestillingMapper bestillingMapper = new BestillingMapper();

	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse,
						   BucketStorage bucketStorage,
						   Regoppslag regoppslag,
						   MetricUpdater metricUpdater) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
		this.regoppslag = regoppslag;
		this.bucketStorage = bucketStorage;
		this.metricUpdater = metricUpdater;
	}

	@Handler
	public byte[] distribuerForsendelseTilSentralPrintService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo, Exchange exchange) {
		final String forsendelseId = distribuerForsendelseTilSentralPrintTo.getForsendelseId();
		HentForsendelseResponseTo hentForsendelseResponseTo = administrerForsendelse.hentForsendelse(forsendelseId);
		final String bestillingsId = hentForsendelseResponseTo.getBestillingsId();
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);
		log.info("qdist009 har mottatt bestilling til print med forsendelseId={}, bestillingsId={}", forsendelseId, bestillingsId);
		validateForsendelseStatus(hentForsendelseResponseTo.getForsendelseStatus());
		final String dokumenttypeIdHoveddokument = getDokumenttypeIdHoveddokument(hentForsendelseResponseTo);
		DokumenttypeInfoTo dokumenttypeInfoTo = dokumentkatalogAdmin.getDokumenttypeInfo(dokumenttypeIdHoveddokument);
		Adresse adresse = getAdresse(hentForsendelseResponseTo, forsendelseId);
		HentPostDestinasjonResponseTo hentPostDestinasjonResponseTo = administrerForsendelse.hentPostDestinasjon(adresse.getLandkode());

		List<DokdistDokument> dokdistDokumentList = getDocumentsFromBucket(hentForsendelseResponseTo);

		Bestilling bestilling = bestillingMapper.createBestilling(hentForsendelseResponseTo, dokumenttypeInfoTo, adresse, hentPostDestinasjonResponseTo);
		String kanalbehandling = bestilling.getBestillingsInfo().getKanal().getBehandling();
		log.info("qdist009 lager bestilling til print med kanalbehandling={}, antall_dokumenter={} for bestillingsId={}, dokumenttypeId={}",
				kanalbehandling, dokdistDokumentList.size(), bestillingsId, dokumenttypeIdHoveddokument);
		String bestillingXmlString = marshalBestillingToXmlString(bestilling);
		List<BestillingEntity> bestillingEntities = createBestillingEntities(bestillingsId, bestillingXmlString, dokdistDokumentList);

		metricUpdater.updateQdist009Metrics(hentPostDestinasjonResponseTo.getPostDestinasjon(), adresse.getLandkode());

		return zipPrintbestillingToBytes(bestillingEntities);
	}

	private Adresse getAdresse(HentForsendelseResponseTo hentForsendelseResponseTo, String forsendelseId) {
		final HentForsendelseResponseTo.PostadresseTo adresseDokdist = hentForsendelseResponseTo.getPostadresse();
		if (adresseDokdist == null) {
			Adresse postadresse = getAdresseFromRegoppslag(hentForsendelseResponseTo);
			administrerForsendelse.oppdaterPostadresse(mapOppdaterPostadresse(forsendelseId, postadresse));
			return postadresse;
		} else {
			return Adresse.builder()
					.adresselinje1(adresseDokdist.getAdresselinje1())
					.adresselinje2(adresseDokdist.getAdresselinje2())
					.adresselinje3(adresseDokdist.getAdresselinje3())
					.landkode(mapLandkode(adresseDokdist.getLandkode()))
					.postnummer(adresseDokdist.getPostnummer())
					.poststed(adresseDokdist.getPoststed())
					.build();
		}
	}

	private String mapLandkode(String landkode) {
		return isBlank(landkode) || UKJENT_LANDKODE.equals(landkode) ? XX_LANDKODE : landkode;
	}

	private Adresse getAdresseFromRegoppslag(HentForsendelseResponseTo hentForsendelseResponseTo) {
		AdresseTo adresseTo = regoppslag.treg002HentAdresse(HentAdresseRequestTo.builder()
				.identifikator(hentForsendelseResponseTo.getMottaker().getMottakerId())
				.type(hentForsendelseResponseTo.getMottaker().getMottakerType())
				.tema(hentForsendelseResponseTo.getTema())
				.build());

		return Adresse.builder()
				.adresselinje1(adresseTo.getAdresselinje1())
				.adresselinje2(adresseTo.getAdresselinje2())
				.adresselinje3(adresseTo.getAdresselinje3())
				.landkode(mapLandkode(adresseTo.getLandkode()))
				.postnummer(adresseTo.getPostnummer())
				.poststed(adresseTo.getPoststed())
				.build();
	}

	/**
	 * Her er rekkefølgen viktig. HentForsendelseResponseTo.dokumenter består av en ordnet liste av dokumenter i rekkefølgen HOVEDDOK, VEDLEGG1, VEDLEGG2, ...
	 * Denne rekkefølgen må bevares slik at bestillingen blir korrekt. Siden vi bruker List.java blir denne rekkefølgen ivaretatt
	 **/
	private List<DokdistDokument> getDocumentsFromBucket(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.map(dokumentTo -> {
					if (isBlank(hentForsendelseResponseTo.getOriginalBestillingsId())) {
						return getDokdistDokument(dokumentTo, hentForsendelseResponseTo.getBestillingsId());
					} else {
						return getDokdistDokument(dokumentTo, hentForsendelseResponseTo.getOriginalBestillingsId());
					}
				})
				.collect(Collectors.toList());
	}

	private DokdistDokument getDokdistDokument(HentForsendelseResponseTo.DokumentTo dokumentTo, String bestillingsId) {
		String jsonPayload = bucketStorage.downloadObject(dokumentTo.getDokumentObjektReferanse(), bestillingsId);
		return deserializeBucketJsonPayloadToDokdistDokument(jsonPayload, dokumentTo.getDokumentObjektReferanse());
	}

	private DokdistDokument deserializeBucketJsonPayloadToDokdistDokument(String jsonPayload, String objektReferanse) {
		DokdistDokument dokdistDokument;
		try {
			dokdistDokument = JsonSerializer.deserialize(jsonPayload, DokdistDokument.class);
			dokdistDokument.setDokumentObjektReferanse(objektReferanse);
		} catch (IllegalStateException e) {
			throw new KunneIkkeDeserialisereBucketJsonPayloadFunctionalException(format("Kunne ikke deserialisere jsonPayload fra bucket for dokument med dokumentobjektreferanse=%s. Dokumentet er ikke persistert til bucket med korrekt format!", objektReferanse));
		}

		if (dokdistDokument.getPdf() == null) {
			throw new NoDocumentFromBucketTechnicalException(format("Det fantes et innslag i bucket på dokumentobjektreferanse=%s, men dette var ikke tilknyttet noe dokument.", objektReferanse));
		}
		return dokdistDokument;
	}

	private OppdaterPostadresseRequest mapOppdaterPostadresse(String forsendelseId, Adresse adresse) {
		return OppdaterPostadresseRequest.builder()
				.forsendelseId(Long.valueOf(forsendelseId))
				.adresselinje1(adresse.getAdresselinje1())
				.adresselinje2(adresse.getAdresselinje2())
				.adresselinje3(adresse.getAdresselinje3())
				.postnummer(adresse.getPostnummer())
				.poststed(adresse.getPoststed())
				.landkode(adresse.getLandkode())
				.build();
	}

}
