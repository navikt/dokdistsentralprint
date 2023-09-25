package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse.Dokument;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterPostadresseRequest;
import no.nav.dokdistsentralprint.consumer.regoppslag.Regoppslag;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfo;
import no.nav.dokdistsentralprint.exception.functional.KunneIkkeDeserialisereBucketJsonPayloadFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.NoDocumentFromBucketTechnicalException;
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
import static no.nav.dokdistsentralprint.qdist009.util.BestillingZipUtil.zipPrintbestillingToBytes;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.createBestillingEntities;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.getDokumenttypeIdHoveddokument;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.validateForsendelsestatus;
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
	private final BestillingMapper bestillingMapper;
	private final BestillingMarshaller bestillingMarshaller;

	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse,
						   BucketStorage bucketStorage,
						   Regoppslag regoppslag,
						   BestillingMapper bestillingMapper, BestillingMarshaller bestillingMarshaller) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
		this.regoppslag = regoppslag;
		this.bucketStorage = bucketStorage;
		this.bestillingMapper = bestillingMapper;
		this.bestillingMarshaller = bestillingMarshaller;
	}

	@Handler
	public byte[] distribuerForsendelseTilSentralPrintService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo, Exchange exchange) {
		final String forsendelseId = distribuerForsendelseTilSentralPrintTo.getForsendelseId();
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);

		final String bestillingsId = hentForsendelseResponse.getBestillingsId();
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);

		log.info("qdist009 har mottatt bestilling til print med forsendelseId={}, bestillingsId={}", forsendelseId, bestillingsId);
		validateForsendelsestatus(hentForsendelseResponse.getForsendelseStatus());

		final String dokumenttypeIdHoveddokument = getDokumenttypeIdHoveddokument(hentForsendelseResponse);
		DokumenttypeInfo dokumenttypeInfo = dokumentkatalogAdmin.hentDokumenttypeInfo(dokumenttypeIdHoveddokument);

		Adresse adresse = getAdresse(hentForsendelseResponse, forsendelseId);
		String postdestinasjon = administrerForsendelse.hentPostdestinasjon(adresse.getLandkode());

		List<DokdistDokument> dokdistDokumentList = getDocumentsFromBucket(hentForsendelseResponse);

		Bestilling bestilling = bestillingMapper.createBestilling(hentForsendelseResponse, dokumenttypeInfo, adresse, postdestinasjon);
		String kanalbehandling = bestilling.getBestillingsInfo().getKanal().getBehandling();
		log.info("qdist009 lager bestilling til print med kanalbehandling={}, antall_dokumenter={} for bestillingsId={}, dokumenttypeId={}",
				kanalbehandling, dokdistDokumentList.size(), bestillingsId, dokumenttypeIdHoveddokument);
		String bestillingXmlString = bestillingMarshaller.marshalBestillingToXmlString(bestilling);
		List<BestillingEntity> bestillingEntities = createBestillingEntities(bestillingsId, bestillingXmlString, dokdistDokumentList);

		return zipPrintbestillingToBytes(bestillingEntities);
	}

	private Adresse getAdresse(HentForsendelseResponse hentForsendelseResponse, String forsendelseId) {
		final HentForsendelseResponse.Postadresse adresseDokdist = hentForsendelseResponse.getPostadresse();
		if (adresseDokdist == null) {
			Adresse postadresse = getAdresseFromRegoppslag(hentForsendelseResponse);
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

	private Adresse getAdresseFromRegoppslag(HentForsendelseResponse hentForsendelseResponse) {
		AdresseTo adresseTo = regoppslag.treg002HentAdresse(HentAdresseRequestTo.builder()
				.identifikator(hentForsendelseResponse.getMottaker().getMottakerId())
				.type(hentForsendelseResponse.getMottaker().getMottakerType())
				.tema(hentForsendelseResponse.getTema())
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
	private List<DokdistDokument> getDocumentsFromBucket(HentForsendelseResponse hentForsendelseResponse) {
		return hentForsendelseResponse.getDokumenter().stream()
				.map(dokument -> {
					if (isBlank(hentForsendelseResponse.getOriginalBestillingsId())) {
						return getDokdistDokument(dokument, hentForsendelseResponse.getBestillingsId());
					} else {
						return getDokdistDokument(dokument, hentForsendelseResponse.getOriginalBestillingsId());
					}
				})
				.collect(Collectors.toList());
	}

	private DokdistDokument getDokdistDokument(Dokument dokument, String bestillingsId) {
		String jsonPayload = bucketStorage.downloadObject(dokument.getDokumentObjektReferanse(), bestillingsId);
		return deserializeBucketJsonPayloadToDokdistDokument(jsonPayload, dokument.getDokumentObjektReferanse());
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
