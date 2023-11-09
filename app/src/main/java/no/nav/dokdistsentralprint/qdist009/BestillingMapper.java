package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.BestillingsInfo;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.printoppdrag.DokumentInfo;
import no.nav.dokdistsentralprint.printoppdrag.Kanal;
import no.nav.dokdistsentralprint.printoppdrag.Mailpiece;
import no.nav.dokdistsentralprint.printoppdrag.Ressurs;
import no.nav.dokdistsentralprint.qdist009.util.Landkoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

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

	public Bestilling createBestilling(HentForsendelseResponse hentForsendelseResponse, DokumenttypeInfo dokumenttypeInfo, String postdestinasjon) {
		Bestilling bestilling = new Bestilling();
		BestillingsInfo bestillingsInfo = new BestillingsInfo();
		bestillingsInfo.setModus(hentForsendelseResponse.getModus());
		bestillingsInfo.setKundeId(KUNDE_ID_NAV_IKT);
		bestillingsInfo.setBestillingsId(hentForsendelseResponse.getBestillingsId());
		bestillingsInfo.setKundeOpprettet(LocalDate.now().toString());
		bestillingsInfo.setDokumentInfo(mapDokumentInfo(postdestinasjon));
		bestillingsInfo.setKanal(mapKanal(dokumenttypeInfo));
		bestilling.setBestillingsInfo(bestillingsInfo);
		bestilling.setMailpiece(mapMailpiece(hentForsendelseResponse, dokumenttypeInfo));
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

	private Mailpiece mapMailpiece(HentForsendelseResponse hentForsendelseResponse, DokumenttypeInfo dokumenttypeInfo) {
		HentForsendelseResponse.Postadresse adresse = hentForsendelseResponse.getPostadresse();

		Mailpiece mailpiece = new Mailpiece();
		mailpiece.setMailpieceId(hentForsendelseResponse.getBestillingsId());
		mailpiece.setRessurs(mapRessurs(hentForsendelseResponse));
		mailpiece.setLandkode(getLandkode(adresse));
		mailpiece.setPostnummer(getPostnummer(adresse));
		mailpiece.getDokument().addAll(mapDokumenter(hentForsendelseResponse, dokumenttypeInfo));
		return mailpiece;
	}

	private Ressurs mapRessurs(HentForsendelseResponse hentForsendelseResponse) {
		Ressurs ressurs = new Ressurs();
		ressurs.setAdresse(addCDataToString(getAdresse(hentForsendelseResponse.getPostadresse(), hentForsendelseResponse.getMottaker().getMottakerNavn())));
		return ressurs;
	}

	private List<Dokument> mapDokumenter(HentForsendelseResponse hentForsendelseResponse, DokumenttypeInfo dokumenttypeInfo) {
		return hentForsendelseResponse.getDokumenter().stream()
				.map(dokumentTo ->
						isMottakerSkattyter(hentForsendelseResponse.getMottaker().getMottakerType()) ?
								mapDokumentSkattyter(hentForsendelseResponse, dokumenttypeInfo, dokumentTo) :
								mapDokumentUtlending(hentForsendelseResponse, dokumenttypeInfo, dokumentTo))
				.collect(Collectors.toList());
	}

	private Dokument mapDokumentSkattyter(HentForsendelseResponse hentForsendelseResponse, DokumenttypeInfo dokumenttypeInfo, HentForsendelseResponse.Dokument dokumentTo) {
		HentForsendelseResponse.Postadresse adresse = hentForsendelseResponse.getPostadresse();

		Dokument dokument = new Dokument();
		dokument.setDokumentType(mapDokumentType(dokumenttypeInfo.getSentralPrintDokumentType()));
		dokument.setDokumentId(dokumentTo.getDokumentObjektReferanse());
		dokument.setSkattyternummer(hentForsendelseResponse.getMottaker().getMottakerId());
		dokument.setNavn(addCDataToString(hentForsendelseResponse.getMottaker().getMottakerNavn()));
		dokument.setLandkode(getLandkode(adresse));
		dokument.setPostnummer(getPostnummer(adresse));
		return dokument;
	}

	private Dokument mapDokumentUtlending(HentForsendelseResponse hentForsendelseResponse, DokumenttypeInfo dokumenttypeInfo, HentForsendelseResponse.Dokument dokumentTo) {
		HentForsendelseResponse.Postadresse adresse = hentForsendelseResponse.getPostadresse();
		Dokument dokument = new Dokument();
		dokument.setDokumentType(mapDokumentType(dokumenttypeInfo.getSentralPrintDokumentType()));
		dokument.setDokumentId(dokumentTo.getDokumentObjektReferanse());
		dokument.setNavn(addCDataToString(hentForsendelseResponse.getMottaker().getMottakerNavn()));
		dokument.setLandkode(getLandkode(adresse));
		dokument.setPostnummer(getPostnummer(adresse));
		return dokument;
	}

	public boolean isMottakerSkattyter(String mottakerType) {
		return MOTTAKERTYPE_PERSON.equals(mottakerType) || MOTTAKERTYPE_ORGANISASJON.equals(mottakerType);
	}

	private String getLandkode(HentForsendelseResponse.Postadresse adresse) {
		if (LANDKODE_NO.equals(adresse.getLandkode())) {
			return null;
		} else {
			return adresse.getLandkode();
		}
	}

	private String getPostnummer(HentForsendelseResponse.Postadresse adresse) {
		if (LANDKODE_NO.equals(adresse.getLandkode())) {
			return adresse.getPostnummer();
		} else {
			return null;
		}
	}

	private String getAdresse(HentForsendelseResponse.Postadresse adresse, String mottakerNavn) {
		return formatAdresseEntity(mottakerNavn) +
			   formatAdresseEntity(adresse.getAdresselinje1()) +
			   formatAdresseEntity(adresse.getAdresselinje2()) +
			   formatAdresseEntity(adresse.getAdresselinje3()) +
			   formatPostnummerAndPoststed(adresse.getPostnummer(), adresse.getPoststed()) +
			   formatLandkode(adresse.getLandkode());
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
