package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.rdist001.HentPostDestinasjonResponseTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.BestillingsInfo;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.printoppdrag.DokumentInfo;
import no.nav.dokdistsentralprint.printoppdrag.Kanal;
import no.nav.dokdistsentralprint.printoppdrag.Mailpiece;
import no.nav.dokdistsentralprint.printoppdrag.Ressurs;
import no.nav.dokdistsentralprint.qdist009.domain.Adresse;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public class BestillingMapper {

	public static final String KUNDE_ID_NAV_IKT = "NAV_IKT";
	public static final String USORTERT = "USORTERT";
	public static final String PRINT = "PRINT";
	private static final String LANDKODE_NO = "NO";
	private static final String MOTTAKERTYPE_PERSON = "PERSON";
	private static final String MOTTAKERTYPE_ORGANISASJON = "ORGANISASJON";

	public Bestilling createBestilling(HentForsendelseResponseTo hentForsendelseResponseTo, DokumenttypeInfoTo dokumenttypeInfoTo, Adresse adresse, HentPostDestinasjonResponseTo hentPostDestinasjonResponseTo) {
		return new Bestilling()
				.withBestillingsInfo(new BestillingsInfo()
						.withModus(hentForsendelseResponseTo.getModus())
						.withKundeId(KUNDE_ID_NAV_IKT)
						.withBestillingsId(hentForsendelseResponseTo.getBestillingsId())
						.withKundeOpprettet(LocalDate.now().toString())
						.withDokumentInfo(new DokumentInfo()
								.withSorteringsfelt(USORTERT)
								.withDestinasjon(hentPostDestinasjonResponseTo.getPostDestinasjon()))
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
						.withDokument(mapDokumenter(hentForsendelseResponseTo, dokumenttypeInfoTo, adresse)));
	}

	private List<Dokument> mapDokumenter(HentForsendelseResponseTo hentForsendelseResponseTo, DokumenttypeInfoTo dokumenttypeInfoTo, Adresse adresse) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.map(dokumentTo ->
						isValidMottakerType(hentForsendelseResponseTo.getMottaker().getMottakerType()) ?
								new Dokument()
										.withDokumentType(dokumenttypeInfoTo.getSentralPrintDokumentType())
										.withDokumentId(dokumentTo.getDokumentObjektReferanse())
										.withSkattyternummer(hentForsendelseResponseTo.getMottaker().getMottakerId())
										.withNavn(addCDataToString(hentForsendelseResponseTo.getMottaker().getMottakerNavn()))
										.withLandkode(getLandkode(adresse))
										.withPostnummer(getPostnummer(adresse)) :
								new Dokument()
										.withDokumentType(dokumenttypeInfoTo.getSentralPrintDokumentType())
										.withDokumentId(dokumentTo.getDokumentObjektReferanse())
										.withNavn(addCDataToString(hentForsendelseResponseTo.getMottaker().getMottakerNavn()))
										.withLandkode(getLandkode(adresse))
										.withPostnummer(getPostnummer(adresse)))
				.collect(Collectors.toList());
	}

	private boolean isValidMottakerType(String mottakerType) {
		return MOTTAKERTYPE_PERSON.equals(mottakerType) || MOTTAKERTYPE_ORGANISASJON.equals(mottakerType);
	}

	private String getLandkode(Adresse adresse) {
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
		return formatAdresseEntity(mottakerNavn) +
				formatAdresseEntity(adresse.getAdresselinje1()) +
				formatAdresseEntity(adresse.getAdresselinje2()) +
				formatAdresseEntity(adresse.getAdresselinje3()) +
				formatPostnummerAndPoststed(adresse.getPostnummer(), adresse.getPoststed()) +
				adresse.getLandkode();
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
