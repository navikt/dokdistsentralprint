package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfo;
import no.nav.dokdistsentralprint.exception.functional.KunneIkkeDeserialisereBucketJsonPayloadFunctionalException;
import no.nav.dokdistsentralprint.exception.technical.NoDocumentFromBucketTechnicalException;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.qdist009.domain.BestillingEntity;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.Dokument;
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
import static org.apache.commons.lang3.StringUtils.isBlank;


@Slf4j
@Service
public class Qdist009Service {
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final PostadresseValidatorOgForsendelseFeilregistrerService postadresseService;
	private final BucketStorage bucketStorage;
	private final BestillingMapper bestillingMapper;
	private final BestillingMarshaller bestillingMarshaller;

	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   PostadresseValidatorOgForsendelseFeilregistrerService postadresseService,
						   BucketStorage bucketStorage,
						   BestillingMapper bestillingMapper,
						   BestillingMarshaller bestillingMarshaller) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.postadresseService = postadresseService;
		this.bucketStorage = bucketStorage;
		this.bestillingMapper = bestillingMapper;
		this.bestillingMarshaller = bestillingMarshaller;
	}

	@Handler
	public byte[] distribuerForsendelseTilSentralPrintService(InternForsendelse internForsendelse, Exchange exchange) {

		final String bestillingsId = internForsendelse.getBestillingsId();
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);

		String postdestinasjon = postadresseService.hentPostdestinasjon(internForsendelse.getPostadresse());

		final String dokumenttypeIdHoveddokument = getDokumenttypeIdHoveddokument(internForsendelse);
		DokumenttypeInfo dokumenttypeInfo = dokumentkatalogAdmin.hentDokumenttypeInfo(dokumenttypeIdHoveddokument);

		List<DokdistDokument> dokdistDokumentList = getDocumentsFromBucket(internForsendelse);

		Bestilling bestilling = bestillingMapper.createBestilling(internForsendelse, dokumenttypeInfo, postdestinasjon);
		String kanalbehandling = bestilling.getBestillingsInfo().getKanal().getBehandling();
		log.info("qdist009 lager bestilling til print med kanalbehandling={}, antall_dokumenter={} for bestillingsId={}, dokumenttypeId={}",
				kanalbehandling, dokdistDokumentList.size(), bestillingsId, dokumenttypeIdHoveddokument);
		String bestillingXmlString = bestillingMarshaller.marshalBestillingToXmlString(bestilling);
		List<BestillingEntity> bestillingEntities = createBestillingEntities(bestillingsId, bestillingXmlString, dokdistDokumentList);

		return zipPrintbestillingToBytes(bestillingEntities);
	}

	/**
	 * Her er rekkefølgen viktig. HentForsendelseResponseTo.dokumenter består av en ordnet liste av dokumenter i rekkefølgen HOVEDDOK, VEDLEGG1, VEDLEGG2, ...
	 * Denne rekkefølgen må bevares slik at bestillingen blir korrekt. Siden vi bruker List.java blir denne rekkefølgen ivaretatt
	 **/
	private List<DokdistDokument> getDocumentsFromBucket(InternForsendelse internForsendelse) {
		return internForsendelse.getDokumenter().stream()
				.map(dokument -> {
					if (isBlank(internForsendelse.getOriginalBestillingsId())) {
						return getDokdistDokument(dokument, internForsendelse.getBestillingsId());
					} else {
						return getDokdistDokument(dokument, internForsendelse.getOriginalBestillingsId());
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

}
