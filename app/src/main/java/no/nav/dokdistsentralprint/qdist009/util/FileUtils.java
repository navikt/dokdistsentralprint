package no.nav.dokdistsentralprint.qdist009.util;

import static java.lang.String.format;

import no.nav.dokdistsentralprint.exception.technical.KunneIkkeMarshalleBestillingTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.KunneIkkeZippeBestillingTechnicalException;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.qdist009.BestillingEntity;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

public final class FileUtils {

	private FileUtils() {
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

	public static String marshalBestillingToXmlString(Bestilling bestilling) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Bestilling.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "printoppdrag-2_2.xsd");
			StringWriter sw = new StringWriter();
			marshaller.marshal(bestilling, sw);
			return sw.toString();
		} catch (JAXBException | IllegalArgumentException e) {
			throw new KunneIkkeMarshalleBestillingTechnicalException("Kunne ikke marshalle bestilling til xmlString");
		}
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
		bestillingEntities.forEach(bestillingEntity -> bestillingEntitiesStr.append(bestillingEntity.toString() + ", "));
		return bestillingEntitiesStr.toString();
	}

}
