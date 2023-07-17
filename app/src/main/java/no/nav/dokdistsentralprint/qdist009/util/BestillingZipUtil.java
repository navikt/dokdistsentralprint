package no.nav.dokdistsentralprint.qdist009.util;

import no.nav.dokdistsentralprint.exception.technical.KunneIkkeZippeBestillingTechnicalException;
import no.nav.dokdistsentralprint.qdist009.domain.BestillingEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;

public final class BestillingZipUtil {

	private BestillingZipUtil() {
		// noop
	}

	public static byte[] zipPrintbestillingToBytes(List<BestillingEntity> bestillingEntities) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			bestillingEntities.forEach(bestillingEntity -> addNewZipEntry(zos, bestillingEntity));
		} catch (IOException e) {
			throw new KunneIkkeZippeBestillingTechnicalException(format("Kunne ikke zippe bestilling for bestillingEntities=%s", printBestillingEntities(bestillingEntities)));
		}
		return baos.toByteArray();
	}

	private static void addNewZipEntry(ZipOutputStream zos, BestillingEntity bestillingEntity) {
		try {
			ZipEntry entry = new ZipEntry(bestillingEntity.getFileName());
			entry.setSize(bestillingEntity.getEntity().length);
			zos.putNextEntry(entry);
			zos.write(bestillingEntity.getEntity());
			zos.closeEntry();
		} catch (IOException | NullPointerException | IllegalArgumentException e) {
			throw new KunneIkkeZippeBestillingTechnicalException(format("Kunne ikke legge til zipEntry for bestillingEntity=%s", bestillingEntity
					.toString()));
		}
	}

	private static String printBestillingEntities(List<BestillingEntity> bestillingEntities) {
		StringBuilder bestillingEntitiesStr = new StringBuilder();
		bestillingEntities.forEach(bestillingEntity -> bestillingEntitiesStr.append(bestillingEntity.toString()).append(", "));
		return bestillingEntitiesStr.toString();
	}

}
