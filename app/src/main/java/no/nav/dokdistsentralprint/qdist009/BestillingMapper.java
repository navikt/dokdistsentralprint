package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.BestillingsInfo;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.printoppdrag.DokumentInfo;
import no.nav.dokdistsentralprint.printoppdrag.Kanal;
import no.nav.dokdistsentralprint.printoppdrag.Mailpiece;
import no.nav.dokdistsentralprint.printoppdrag.Ressurs;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import no.nav.dokdistsentralprint.qdist009.util.Landkoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substring;

@Slf4j
@Component
public class BestillingMapper {

	public static final String KUNDE_ID_NAV_IKT = "NAV_IKT";
	public static final String USORTERT = "USORTERT";
	public static final String PRINT = "PRINT";
	private static final String LANDKODE_NO = "NO";
	private static final String MOTTAKERTYPE_PERSON = "PERSON";
	private static final String MOTTAKERTYPE_ORGANISASJON = "ORGANISASJON";
	private static final String KONVOLUTT_MED_VINDU = "X";
	private static final String NAV_STANDARD = "NAV_STANDARD";

	public Bestilling createBestilling(InternForsendelse internForsendelse, DokumenttypeInfo dokumenttypeInfo, String postdestinasjon) {
		Bestilling bestilling = new Bestilling();
		BestillingsInfo bestillingsInfo = new BestillingsInfo();
		bestillingsInfo.setModus(internForsendelse.getModus());
		bestillingsInfo.setKundeId(KUNDE_ID_NAV_IKT);
		bestillingsInfo.setBestillingsId(internForsendelse.getBestillingsId());
		bestillingsInfo.setKundeOpprettet(LocalDate.now().toString());
		bestillingsInfo.setDokumentInfo(mapDokumentInfo(postdestinasjon));
		bestillingsInfo.setKanal(mapKanal(dokumenttypeInfo));
		bestilling.setBestillingsInfo(bestillingsInfo);
		bestilling.setMailpiece(mapMailpiece(internForsendelse, dokumenttypeInfo));
		return bestilling;
	}

	private Kanal mapKanal(DokumenttypeInfo dokumenttypeInfo) {
		Kanal kanal = new Kanal();
		kanal.setType(PRINT);
		kanal.setBehandling(getBehandling(dokumenttypeInfo));
		return kanal;
	}

	private static DokumentInfo mapDokumentInfo(String postdestinasjon) {
		DokumentInfo dokumentInfo = new DokumentInfo();
		dokumentInfo.setSorteringsfelt(USORTERT);
		dokumentInfo.setDestinasjon(postdestinasjon);
		return dokumentInfo;
	}

	private Mailpiece mapMailpiece(InternForsendelse internForsendelse, DokumenttypeInfo dokumenttypeInfo) {
		InternForsendelse.Postadresse adresse = internForsendelse.getPostadresse();

		Mailpiece mailpiece = new Mailpiece();
		mailpiece.setMailpieceId(internForsendelse.getBestillingsId());
		mailpiece.setRessurs(mapRessurs(internForsendelse));
		mailpiece.setLandkode(getLandkode(adresse));
		mailpiece.setPostnummer(getPostnummer(adresse));
		mailpiece.getDokument().addAll(mapDokumenter(internForsendelse, dokumenttypeInfo));
		return mailpiece;
	}

	private Ressurs mapRessurs(InternForsendelse internForsendelse) {
		Ressurs ressurs = new Ressurs();
		ressurs.setAdresse(addCDataToString(getAdresse(internForsendelse.getPostadresse(), internForsendelse.getMottaker().getMottakerNavn())));
		return ressurs;
	}

	private List<Dokument> mapDokumenter(InternForsendelse internForsendelse, DokumenttypeInfo dokumenttypeInfo) {
		return internForsendelse.getDokumenter().stream()
				.map(dokumentTo ->
						isMottakerSkattyter(internForsendelse.getMottaker().getMottakerType()) ?
								mapDokumentSkattyter(internForsendelse, dokumenttypeInfo, dokumentTo) :
								mapDokumentUtlending(internForsendelse, dokumenttypeInfo, dokumentTo))
				.collect(Collectors.toList());
	}

	private Dokument mapDokumentSkattyter(InternForsendelse internForsendelse, DokumenttypeInfo dokumenttypeInfo, InternForsendelse.Dokument dokumentTo) {
		InternForsendelse.Postadresse adresse = internForsendelse.getPostadresse();

		Dokument dokument = new Dokument();
		dokument.setDokumentType(mapDokumentType(dokumenttypeInfo.getSentralPrintDokumentType()));
		dokument.setDokumentId(dokumentTo.getDokumentObjektReferanse());
		dokument.setSkattyternummer(internForsendelse.getMottaker().getMottakerId());
		dokument.setNavn(addCDataToString(internForsendelse.getMottaker().getMottakerNavn()));
		dokument.setLandkode(getLandkode(adresse));
		dokument.setPostnummer(getPostnummer(adresse));
		return dokument;
	}

	private Dokument mapDokumentUtlending(InternForsendelse internForsendelse, DokumenttypeInfo dokumenttypeInfo, InternForsendelse.Dokument dokumentTo) {
		InternForsendelse.Postadresse adresse = internForsendelse.getPostadresse();
		Dokument dokument = new Dokument();
		dokument.setDokumentType(mapDokumentType(dokumenttypeInfo.getSentralPrintDokumentType()));
		dokument.setDokumentId(dokumentTo.getDokumentObjektReferanse());
		dokument.setNavn(addCDataToString(internForsendelse.getMottaker().getMottakerNavn()));
		dokument.setLandkode(getLandkode(adresse));
		dokument.setPostnummer(getPostnummer(adresse));
		return dokument;
	}

	public boolean isMottakerSkattyter(String mottakerType) {
		return MOTTAKERTYPE_PERSON.equals(mottakerType) || MOTTAKERTYPE_ORGANISASJON.equals(mottakerType);
	}

	private String getLandkode(InternForsendelse.Postadresse adresse) {
		if (LANDKODE_NO.equals(adresse.getLandkode())) {
			return null;
		} else {
			return adresse.getLandkode();
		}
	}

	private String getPostnummer(InternForsendelse.Postadresse adresse) {
		if (LANDKODE_NO.equals(adresse.getLandkode())) {
			return adresse.getPostnummer();
		} else {
			return null;
		}
	}

	private String getAdresse(InternForsendelse.Postadresse adresse, String mottakerNavn) {
		return formatNavnOgAdresseEntity(mottakerNavn) +
				formatNavnOgAdresseEntity(adresse.getAdresselinje1()) +
				formatNavnOgAdresseEntity(adresse.getAdresselinje2()) +
				formatNavnOgAdresseEntity(adresse.getAdresselinje3()) +
				formatPostnummerAndPoststed(adresse.getPostnummer(), adresse.getPoststed()) +
				formatLandkode(adresse.getLandkode());
	}

	private String formatNavnOgAdresseEntity(String entity) {
		if (entity == null || entity.isEmpty()) {
			return "";
		} else {
			return format("%s\r", substring(entity, 0, 128));
		}
	}

	private String formatPostnummerAndPoststed(String postnummer, String poststed) {
		if (postnummer == null || postnummer.isEmpty() || poststed == null || poststed.isEmpty()) {
			return "";
		} else {
			return format("%s %s\r", postnummer, poststed);
		}
	}

	private String formatLandkode(String landkode) {
		if (landkode == null || LANDKODE_NO.equals(landkode)) {
			return "";
		} else {
			try {
				return Landkoder.valueOf(landkode).getLandnavn();
			} catch (IllegalArgumentException e) {
				log.error("Mapping av landkode={} til landnavn feilet, da landkoden ikke ligger i Landkoder-enumen. " +
						"Hør med teamet om landkoden bør legges inn.", landkode, e);
				return landkode;
			}
		}
	}

	private String addCDataToString(String s) {
		return format("<![CDATA[%s]]>", s);
	}

	private String getBehandling(DokumenttypeInfo dokumenttypeInfo) {
		return format("%s_%s_%s", dokumenttypeInfo.getPortoklasse(), mapKonvoluttvinduType(dokumenttypeInfo), getPlex(dokumenttypeInfo
				.isTosidigprint()));
	}

	private String mapKonvoluttvinduType(DokumenttypeInfo dokumenttypeInfo) {
		return isBlank(dokumenttypeInfo.getKonvoluttvinduType()) ? KONVOLUTT_MED_VINDU : dokumenttypeInfo.getKonvoluttvinduType();
	}

	private String mapDokumentType(String sentralPrintDokumentType) {
		return isBlank(sentralPrintDokumentType) ? NAV_STANDARD : sentralPrintDokumentType;
	}

	private String getPlex(boolean tosidigPrint) {
		if (tosidigPrint) {
			return "D";
		} else {
			return "S";
		}
	}

}
