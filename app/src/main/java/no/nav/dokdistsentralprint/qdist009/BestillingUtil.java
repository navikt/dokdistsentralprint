package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.BestillingsInfo;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.printoppdrag.DokumentInfo;
import no.nav.dokdistsentralprint.printoppdrag.Kanal;
import no.nav.dokdistsentralprint.printoppdrag.Mailpiece;
import no.nav.dokdistsentralprint.printoppdrag.Ressurs;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class BestillingUtil {

	public static final String KUNDE_ID_NAV_IKT = "NAV_IKT";
	public static final String USORTERT = "USORTERT";
	public static final String PRINT = "PRINT";
	public static final String LANDKODE_NO = "NO";

	public Bestilling createBestilling(HentForsendelseResponseTo hentForsendelseResponseTo, DokumenttypeInfoTo dokumenttypeInfoTo) {
		return new Bestilling()
				.withBestillingsInfo(new BestillingsInfo()
						.withModus(hentForsendelseResponseTo.getModus())
						.withKundeId(KUNDE_ID_NAV_IKT)
						.withBestillingsId(hentForsendelseResponseTo.getBestillingsId())
						.withKundeOpprettet(LocalDate.now().toString())
						.withDokumentInfo(new DokumentInfo()
								.withSorteringsfelt(USORTERT)
								.withDestinasjon("DEST")) //todo fix!
						.withKanal(new Kanal()
								.withType(PRINT)
								.withBehandling(getBehandling(dokumenttypeInfoTo))))
				.withMailpiece(new Mailpiece()
						.withMailpieceId(hentForsendelseResponseTo.getBestillingsId())
						.withRessurs(new Ressurs()
								.withAdresse(addCDataToString(getAdresse(hentForsendelseResponseTo))))
						.withLandkode(getLandkode(hentForsendelseResponseTo.getPostadresse()))
						.withPostnummer(getPostnummer(hentForsendelseResponseTo.getPostadresse()))
						.withDokument(hentForsendelseResponseTo.getDokumenter().stream()
								.map(dokumentTo -> new Dokument()
										.withDokumentType(dokumenttypeInfoTo.getSentralPrintDokumentType())
										.withDokumentId(dokumentTo.getDokumentObjektReferanse())
										.withSkattyternummer(hentForsendelseResponseTo.getMottaker().getMottakerId())
										.withNavn(addCDataToString(hentForsendelseResponseTo.getMottaker().getMottakerNavn()))
										.withLandkode(getLandkode(hentForsendelseResponseTo.getPostadresse()))
										.withPostnummer(getPostnummer(hentForsendelseResponseTo.getPostadresse()))) //Todo Verifiser korrekt oppførsel
								.collect(Collectors.toList())));
	}

	public void zipFile(File file) throws Throwable {
		// defined in java.net.JarURLConnection
		Path resourceDirectoryPath = Paths.get("src", "test", "resources", "test.zip");
		Path resourcepathBestilling = Paths.get("src", "test", "resources");
		String dirNameZip = resourceDirectoryPath.toAbsolutePath().toString();
		String dirNameBestilling = resourcepathBestilling.toAbsolutePath().toString();


//		FileOutputStream fout = new FileOutputStream(dirNameZip);
		try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(file))) {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(resourcepathBestilling);
			dirStream.forEach(path -> {
				try {
					addToZipFile(path, zipStream);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

	}

	private void addToZipFile(Path file, ZipOutputStream zipStream) throws Exception {
		String inputFileName = file.toFile().getPath();
		try (FileInputStream inputStream = new FileInputStream(inputFileName)) {

			// create a new ZipEntry, which is basically another file
			// within the archive. We omit the path from the filename
			ZipEntry entry = new ZipEntry(file.toFile().getName());
			entry.setCreationTime(FileTime.fromMillis(file.toFile().lastModified()));
			entry.setComment("Created by TheCodersCorner");
			zipStream.putNextEntry(entry);


			// Now we copy the existing file into the zip archive. To do
			// this we write into the zip stream, the call to putNextEntry
			// above prepared the stream, we now write the bytes for this
			// entry. For another source such as an in memory array, you'd
			// just change where you read the information from.
			byte[] readBuffer = new byte[2048];
			int amountRead;
			int written = 0;

			while ((amountRead = inputStream.read(readBuffer)) > 0) {
				zipStream.write(readBuffer, 0, amountRead);
				written += amountRead;
			}


		} catch (IOException e) {
			throw new Exception("Unable to process " + inputFileName, e);
		}
	}

	public File marshalBestillingToXmlFile(Bestilling bestilling) throws JAXBException, IOException {
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

	public String marshalBestillingToXmlString(Bestilling bestilling) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Bestilling.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "printoppdrag-2_2.xsd");
		StringWriter sw = new StringWriter();
		marshaller.marshal(bestilling, sw);
		return sw.toString();
	}

	public String getLandkode(HentForsendelseResponseTo.PostadresseTo postadresseTo) {
		if (LANDKODE_NO.equals(postadresseTo.getLandkode())) {
			return null;
		} else {
			return postadresseTo.getLandkode();
		}
	}

	private String getPostnummer(HentForsendelseResponseTo.PostadresseTo postadresseTo) {
		if (LANDKODE_NO.equals(postadresseTo.getLandkode())) {
			return postadresseTo.getLandkode();
		} else {
			return null;
		}
	}

	private String getAdresse(HentForsendelseResponseTo hentForsendelseResponseTo) {
		HentForsendelseResponseTo.PostadresseTo postadresseTo = hentForsendelseResponseTo.getPostadresse();
		if (postadresseTo == null) {
			return "";
		} else {
			return formatAdresseEntity(hentForsendelseResponseTo.getMottaker().getMottakerNavn()) +
					formatAdresseEntity(postadresseTo.getAdresselinje1()) +
					formatAdresseEntity(postadresseTo.getAdresselinje2()) +
					formatAdresseEntity(postadresseTo.getAdresselinje3()) +
					formatPostnummerAndPoststed(postadresseTo.getPostnummer(), postadresseTo.getPoststed()) +
					postadresseTo.getLandkode();
		}
	}

	private String formatAdresseEntity(String entity) {
		if (entity == null || entity.isEmpty()) {
			return "";
		} else {
			return format("%s\r", entity);
		}
	}

	private String formatPostnummerAndPoststed(String postnummer, String poststed) {
		if (postnummer == null || postnummer.isEmpty() || poststed == null || poststed.isEmpty()) {
			return "";
		} else {
			return format("%s %s\r", postnummer, poststed);
		}
	}

	private String addCDataToString(String s) {
		return format("<![CDATA[%s]]>", s);
	}

	private String getBehandling(DokumenttypeInfoTo dokumenttypeInfoTo) {
		return format("%s_%s_%s", dokumenttypeInfoTo.getPortoklasse(), dokumenttypeInfoTo.getKonvoluttvinduType(), getPlex(dokumenttypeInfoTo
				.isTosidigprint()));
	}

	private String getPlex(boolean tosidigPrint) {
		if (tosidigPrint) {
			return "D";
		} else {
			return "S";
		}
	}

}
