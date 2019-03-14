package no.nav.dokdistsentralprint.qdist009.util;

import static java.lang.String.format;

import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.qdist009.BestillingEntity;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

public final class FileUtils {

	private FileUtils() {
	}

	public static byte[] zipBytes(List<BestillingEntity> bestillingEntities) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		bestillingEntities.forEach(bestillingEntity -> {
			try {
				ZipEntry entry = new ZipEntry(bestillingEntity.getFileName());
				entry.setSize(bestillingEntity.getEntity().length);
				zos.putNextEntry(entry);
				zos.write(bestillingEntity.getEntity());
				zos.closeEntry();
			} catch (Exception e) {
				//fixme
			}
		});
		zos.close();
		Path resourcepathBestilling = Paths.get("app", "src", "test", "resources");
		String dirNameBestilling = resourcepathBestilling.toAbsolutePath().toString();
		File file = new File(dirNameBestilling + "/TEST.zip");
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(baos.toByteArray());
		}

		return baos.toByteArray();
	}

	public static byte[] zipBytesSingle(String filename, byte[] input) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);
		ZipEntry entry = new ZipEntry(filename);
		entry.setSize(input.length);
		zos.putNextEntry(entry);
		zos.write(input);
		zos.closeEntry();
		zos.close();

		Path resourcepathBestilling = Paths.get("src", "test", "resources");
		String dirNameBestilling = resourcepathBestilling.toAbsolutePath().toString();
		File file = new File(dirNameBestilling + "/TEST.zip");
		new FileOutputStream(file).write(baos.toByteArray());

		return baos.toByteArray();
	}


	public static File marshalBestillingToXmlFile(Bestilling bestilling) throws JAXBException, IOException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Bestilling.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "printoppdrag-2_2.xsd");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		String fileName = format("%s.xml", bestilling.getBestillingsInfo().getBestillingsId());
		Path resourceDirectoryPath = Paths.get("src", "test", "resources");
		String dirName = resourceDirectoryPath.toAbsolutePath().toString();
		File dir = new File(dirName);
		File actualFile = new File(dir, fileName);

		marshaller.marshal(bestilling, new FileOutputStream(actualFile));
		return actualFile;
	}

	public static String marshalBestillingToXmlString(Bestilling bestilling) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Bestilling.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "printoppdrag-2_2.xsd");
			StringWriter sw = new StringWriter();
			marshaller.marshal(bestilling, sw);
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException(); //Todo fix
		}

	}

}
