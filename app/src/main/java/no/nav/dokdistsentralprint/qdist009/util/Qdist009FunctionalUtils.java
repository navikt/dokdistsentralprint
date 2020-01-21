package no.nav.dokdistsentralprint.qdist009.util;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.constants.DomainConstants.FORSENDELSE_STATUS_KLAR_FOR_DIST;
import static no.nav.dokdistsentralprint.constants.DomainConstants.HOVEDDOKUMENT;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistsentralprint.qdist009.domain.BestillingEntity;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class Qdist009FunctionalUtils {
	private Qdist009FunctionalUtils() {
	}

	public static void validateForsendelseStatus(String forsendelseStatus) {
		if (!FORSENDELSE_STATUS_KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(format("ForsendelseStatus må være %s. Fant forsendelseStatus=%s", FORSENDELSE_STATUS_KLAR_FOR_DIST, forsendelseStatus));
		}
	}

	public static String getDokumenttypeIdHoveddokument(HentForsendelseResponseTo hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(dokumentTo -> HOVEDDOKUMENT.equals(dokumentTo.getTilknyttetSom()))
				.map(HentForsendelseResponseTo.DokumentTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

	public static List<BestillingEntity> createBestillingEntities(String bestillingId, String bestillingXml, List<DokdistDokument> dokdistDokumentList) {
		List<BestillingEntity> bestillingEntities = new ArrayList<>();
		BestillingEntity bestillingXmlEntity = createBestillingXmlEntity(bestillingId, bestillingXml);
		List<BestillingEntity> documentEntities = createDocumentEntities(dokdistDokumentList);
		bestillingEntities.add(bestillingXmlEntity);
		bestillingEntities.addAll(documentEntities);
		return bestillingEntities;
	}

	private static BestillingEntity createBestillingXmlEntity(String bestillingId, String bestillingXml) {
		return BestillingEntity.builder()
				.fileName(format("%s.xml", bestillingId))
				.entity(bestillingXml.getBytes())
				.build();
	}

	private static List<BestillingEntity> createDocumentEntities(List<DokdistDokument> dokdistDokumentList) {
		return dokdistDokumentList.stream()
				.map(dokdistDokument -> (BestillingEntity.builder()
						.fileName(format("%s.pdf", dokdistDokument.getDokumentObjektReferanse()))
						.entity(dokdistDokument.getPdf())
						.build()))
				.collect(Collectors.toList());
	}


}
