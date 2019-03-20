package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelseConsumer;
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

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class BestillingMapper {

	public static final String KUNDE_ID_NAV_IKT = "NAV_IKT";
	public static final String USORTERT = "USORTERT";
	public static final String PRINT = "PRINT";
	public static final String LANDKODE_NO = "NO";

	@Inject
	private AdministrerForsendelseConsumer administrerForsendelseConsumer;

	public Bestilling createBestilling(HentForsendelseResponseTo hentForsendelseResponseTo, DokumenttypeInfoTo dokumenttypeInfoTo, Adresse adresse, String postDestinasjon) {
		return new Bestilling()
				.withBestillingsInfo(new BestillingsInfo()
						.withModus(hentForsendelseResponseTo.getModus())
						.withKundeId(KUNDE_ID_NAV_IKT)
						.withBestillingsId(hentForsendelseResponseTo.getBestillingsId())
						.withKundeOpprettet(LocalDate.now().toString())
						.withDokumentInfo(new DokumentInfo()
								.withSorteringsfelt(USORTERT)
								.withDestinasjon(postDestinasjon))
						.withKanal(new Kanal()
								.withType(PRINT)
								.withBehandling(getBehandling(dokumenttypeInfoTo))))
				.withMailpiece(new Mailpiece()
						.withMailpieceId(hentForsendelseResponseTo.getBestillingsId())
						.withRessurs(new Ressurs()
								.withAdresse(addCDataToString(getAdresse(adresse, hentForsendelseResponseTo.getMottaker()
										.getMottakerNavn()))))
						.withLandkode(getLandkode(adresse))
						.withPostnummer(getPostnummer(adresse))
						.withDokument(hentForsendelseResponseTo.getDokumenter().stream()
								.map(dokumentTo -> new Dokument()
										.withDokumentType(dokumenttypeInfoTo.getSentralPrintDokumentType())
										.withDokumentId(dokumentTo.getDokumentObjektReferanse())
										.withSkattyternummer(hentForsendelseResponseTo.getMottaker().getMottakerId())
										.withNavn(addCDataToString(hentForsendelseResponseTo.getMottaker().getMottakerNavn()))
										.withLandkode(getLandkode(adresse))
										.withPostnummer(getPostnummer(adresse)))
								.collect(Collectors.toList())));
	}

	public String getLandkode(Adresse adresse) {
		if (LANDKODE_NO.equals(adresse.getLandkode())) {
			return null;
		} else {
			return adresse.getLandkode();
		}
	}

	private String getPostnummer(Adresse adresse) {
		if (LANDKODE_NO.equals(adresse.getLandkode())) {
			return adresse.getPostnummer();
		} else {
			return null;
		}
	}

	private String getAdresse(Adresse adresse, String mottakerNavn) {
		if (adresse == null) { //todo mulig??
			return "";
		} else {
			return formatAdresseEntity(mottakerNavn) +
					formatAdresseEntity(adresse.getAdresselinje1()) +
					formatAdresseEntity(adresse.getAdresselinje2()) +
					formatAdresseEntity(adresse.getAdresselinje3()) +
					formatPostnummerAndPoststed(adresse.getPostnummer(), adresse.getPoststed()) +
					adresse.getLandkode();
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
