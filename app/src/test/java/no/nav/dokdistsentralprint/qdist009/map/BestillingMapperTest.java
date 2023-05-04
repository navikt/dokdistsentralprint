package no.nav.dokdistsentralprint.qdist009.map;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentPostdestinasjonResponse;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.qdist009.BestillingMapper;
import no.nav.dokdistsentralprint.qdist009.domain.Adresse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.KUNDE_ID_NAV_IKT;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.PRINT;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.USORTERT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestillingMapperTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String MODUS = "modus";
	private static final String MOTTAKER_NAVN = "mottakerNavn";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND_NO = "NO";
	private static final String LAND_SE = "SE";
	private static final String LAND_SE_NAVN = "SVERIGE";
	private static final String OBJEKT_REFERANSE_HOVEDDOK = "objektreferanseHoveddok";
	private static final String DOKUMENTTYPE_ID_HOVEDDOK = "dokumenttypeIdHoveddok";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";

	private static final String OBJEKT_REFERANSE_VEDLEGG1 = "objektreferanseVedlegg1";
	private static final String DOKUMENTTYPE_ID_VEDLEGG1 = "dokumenttypeIdVedlegg1";
	private static final String OBJEKT_REFERANSE_VEDLEGG2 = "objektreferanseVedlegg2";
	private static final String DOKUMENTTYPE_ID_VEDLEGG2 = "dokumenttypeIdVedlegg2";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";

	private static final String KONVOLUTTVINDU_TYPE = "konvoluttvinduType";
	private static final String PORTOKLASSE = "portoklasse";
	private static final String SENTRALPRINT_DOKTYPE = "sentPrintDokType";
	private static final String POST_DESTINASJON_INNLAND = "INNLAND";
	private static final boolean TOSIDIG_PRINT_TRUE = true;
	private static final boolean TOSIDIG_PRINT_FALSE = false;
	private static final String MOTTAKERTYPE_PERSON = "PERSON";
	private static final String MOTTAKERTYPE_ORGANISASJON = "ORGANISASJON";
	private static final String MOTTAKERTYPE_UKJENT = "UKJENT";
	private static final String CDATA_MOTTAKER_NAVN = "<![CDATA[" + MOTTAKER_NAVN + "]]>";
	private static final String NAV_STANDARD = "NAV_STANDARD";

	private final BestillingMapper bestillingMapper = new BestillingMapper();


	@Test
	void shouldMap() {

		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_TRUE),
				createAdresse(LAND_NO),
				createHentPostdestinasjon());


		assertEquals(MODUS, bestilling.getBestillingsInfo().getModus());
		assertEquals(KUNDE_ID_NAV_IKT, bestilling.getBestillingsInfo().getKundeId());
		assertEquals(BESTILLINGS_ID, bestilling.getBestillingsInfo().getBestillingsId());
		assertEquals(LocalDate.now().toString(), bestilling.getBestillingsInfo().getKundeOpprettet());

		assertEquals(USORTERT, bestilling.getBestillingsInfo().getDokumentInfo().getSorteringsfelt());
		assertEquals(POST_DESTINASJON_INNLAND, bestilling.getBestillingsInfo().getDokumentInfo().getDestinasjon());
		assertEquals(PRINT, bestilling.getBestillingsInfo().getKanal().getType());
		assertEquals(PORTOKLASSE + "_" + KONVOLUTTVINDU_TYPE + "_D", bestilling.getBestillingsInfo().getKanal().getBehandling());

		assertEquals(BESTILLINGS_ID, bestilling.getMailpiece().getMailpieceId());
		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertNull(bestilling.getMailpiece().getLandkode());
		assertEquals(POSTNUMMER, bestilling.getMailpiece().getPostnummer());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().get(0);

		assertEquals(SENTRALPRINT_DOKTYPE, hovedDokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hovedDokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hovedDokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hovedDokument.getSkattyternummer());
		assertNull(hovedDokument.getLandkode());
		assertEquals(POSTNUMMER, hovedDokument.getPostnummer());

		Dokument vedlegg1 = bestilling.getMailpiece().getDokument().get(1);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg1.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg1.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG1, vedlegg1.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg1.getSkattyternummer());
		assertNull(vedlegg1.getLandkode());
		assertEquals(POSTNUMMER, vedlegg1.getPostnummer());

		Dokument vedlegg2 = bestilling.getMailpiece().getDokument().get(2);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg2.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg2.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG2, vedlegg2.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg2.getSkattyternummer());
		assertNull(vedlegg2.getLandkode());
		assertEquals(POSTNUMMER, vedlegg2.getPostnummer());
	}

	@Test
	void shouldMapDefaultValueWhenSentralPrintDokumentTypeIkkeSettPåDokumenttypeInfo() {

		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoToUtenSentralPrintDokumentType(TOSIDIG_PRINT_TRUE),
				createAdresse(LAND_NO),
				createHentPostdestinasjon());


		assertEquals(MODUS, bestilling.getBestillingsInfo().getModus());
		assertEquals(KUNDE_ID_NAV_IKT, bestilling.getBestillingsInfo().getKundeId());
		assertEquals(BESTILLINGS_ID, bestilling.getBestillingsInfo().getBestillingsId());
		assertEquals(LocalDate.now().toString(), bestilling.getBestillingsInfo().getKundeOpprettet());

		assertEquals(USORTERT, bestilling.getBestillingsInfo().getDokumentInfo().getSorteringsfelt());
		assertEquals(POST_DESTINASJON_INNLAND, bestilling.getBestillingsInfo().getDokumentInfo().getDestinasjon());
		assertEquals(PRINT, bestilling.getBestillingsInfo().getKanal().getType());
		assertEquals(PORTOKLASSE + "_" + KONVOLUTTVINDU_TYPE + "_D", bestilling.getBestillingsInfo().getKanal().getBehandling());

		assertEquals(BESTILLINGS_ID, bestilling.getMailpiece().getMailpieceId());
		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertNull(bestilling.getMailpiece().getLandkode());
		assertEquals(POSTNUMMER, bestilling.getMailpiece().getPostnummer());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().get(0);

		assertEquals(NAV_STANDARD, hovedDokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hovedDokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hovedDokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hovedDokument.getSkattyternummer());
		assertNull(hovedDokument.getLandkode());
		assertEquals(POSTNUMMER, hovedDokument.getPostnummer());

		Dokument vedlegg1 = bestilling.getMailpiece().getDokument().get(1);

		assertEquals(NAV_STANDARD, vedlegg1.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg1.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG1, vedlegg1.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg1.getSkattyternummer());
		assertNull(vedlegg1.getLandkode());
		assertEquals(POSTNUMMER, vedlegg1.getPostnummer());

		Dokument vedlegg2 = bestilling.getMailpiece().getDokument().get(2);

		assertEquals(NAV_STANDARD, vedlegg2.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg2.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG2, vedlegg2.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg2.getSkattyternummer());
		assertNull(vedlegg2.getLandkode());
		assertEquals(POSTNUMMER, vedlegg2.getPostnummer());
	}

	@Test
	void shouldMapKonvoluttTypeToXWhenKonvoluttvinduTypeIkkeSettPåDokumenttypeInfo() {

		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoUtenKonvoluttvinduType(TOSIDIG_PRINT_TRUE),
				createAdresse(LAND_NO),
				createHentPostdestinasjon());

		assertEquals(MODUS, bestilling.getBestillingsInfo().getModus());
		assertEquals(KUNDE_ID_NAV_IKT, bestilling.getBestillingsInfo().getKundeId());
		assertEquals(BESTILLINGS_ID, bestilling.getBestillingsInfo().getBestillingsId());
		assertEquals(LocalDate.now().toString(), bestilling.getBestillingsInfo().getKundeOpprettet());

		assertEquals(USORTERT, bestilling.getBestillingsInfo().getDokumentInfo().getSorteringsfelt());
		assertEquals(POST_DESTINASJON_INNLAND, bestilling.getBestillingsInfo().getDokumentInfo().getDestinasjon());
		assertEquals(PRINT, bestilling.getBestillingsInfo().getKanal().getType());
		assertEquals(PORTOKLASSE + "_" + "X" + "_D", bestilling.getBestillingsInfo().getKanal().getBehandling());

		assertEquals(BESTILLINGS_ID, bestilling.getMailpiece().getMailpieceId());
		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertNull(bestilling.getMailpiece().getLandkode());
		assertEquals(POSTNUMMER, bestilling.getMailpiece().getPostnummer());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().get(0);

		assertEquals(SENTRALPRINT_DOKTYPE, hovedDokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hovedDokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hovedDokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hovedDokument.getSkattyternummer());
		assertNull(hovedDokument.getLandkode());
		assertEquals(POSTNUMMER, hovedDokument.getPostnummer());

		Dokument vedlegg1 = bestilling.getMailpiece().getDokument().get(1);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg1.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg1.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG1, vedlegg1.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg1.getSkattyternummer());
		assertNull(vedlegg1.getLandkode());
		assertEquals(POSTNUMMER, vedlegg1.getPostnummer());

		Dokument vedlegg2 = bestilling.getMailpiece().getDokument().get(2);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg2.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg2.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG2, vedlegg2.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg2.getSkattyternummer());
		assertNull(vedlegg2.getLandkode());
		assertEquals(POSTNUMMER, vedlegg2.getPostnummer());
	}

	@Test
	void shouldMapBestillingWithTosidigPrintFalse() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_ORGANISASJON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createAdresse(LAND_NO),
				createHentPostdestinasjon());

		assertEquals(PORTOKLASSE + "_" + KONVOLUTTVINDU_TYPE + "_S", bestilling.getBestillingsInfo().getKanal().getBehandling());
	}


	@Test
	void shouldMapBestillingWithUtenlandsLandkode() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_ORGANISASJON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createAdresse(LAND_SE),
				createHentPostdestinasjon());


		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + LAND_SE_NAVN + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertEquals(LAND_SE, bestilling.getMailpiece().getLandkode());
		assertNull(bestilling.getMailpiece().getPostnummer());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().iterator().next();

		assertEquals(SENTRALPRINT_DOKTYPE, hovedDokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hovedDokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hovedDokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hovedDokument.getSkattyternummer());
		assertEquals(LAND_SE, hovedDokument.getLandkode());
		assertNull(hovedDokument.getPostnummer());

		Dokument vedlegg1 = bestilling.getMailpiece().getDokument().get(1);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg1.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg1.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG1, vedlegg1.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg1.getSkattyternummer());
		assertNull(vedlegg1.getPostnummer());
		assertEquals(LAND_SE, vedlegg1.getLandkode());

		Dokument vedlegg2 = bestilling.getMailpiece().getDokument().get(2);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg2.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg2.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG2, vedlegg2.getDokumentId());
		assertEquals(MOTTAKER_ID, vedlegg2.getSkattyternummer());
		assertNull(vedlegg2.getPostnummer());
		assertEquals(LAND_SE, vedlegg2.getLandkode());
	}

	@Test
	void shouldNotMapSkatteyternummerInneBestillingWhenMottakerTypeErIkkeOrganizationEllerPerson() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_UKJENT),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createAdresse(LAND_SE),
				createHentPostdestinasjon());


		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + LAND_SE_NAVN + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertEquals(LAND_SE, bestilling.getMailpiece().getLandkode());
		assertNull(bestilling.getMailpiece().getPostnummer());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().iterator().next();

		assertEquals(SENTRALPRINT_DOKTYPE, hovedDokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hovedDokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hovedDokument.getDokumentId());
		assertNull(hovedDokument.getSkattyternummer());
		assertEquals(LAND_SE, hovedDokument.getLandkode());
		assertNull(hovedDokument.getPostnummer());

		Dokument vedlegg1 = bestilling.getMailpiece().getDokument().get(1);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg1.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg1.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG1, vedlegg1.getDokumentId());
		assertNull(vedlegg1.getSkattyternummer());
		assertNull(vedlegg1.getPostnummer());
		assertEquals(LAND_SE, vedlegg1.getLandkode());

		Dokument vedlegg2 = bestilling.getMailpiece().getDokument().get(2);

		assertEquals(SENTRALPRINT_DOKTYPE, vedlegg2.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, vedlegg2.getNavn());
		assertEquals(OBJEKT_REFERANSE_VEDLEGG2, vedlegg2.getDokumentId());
		assertNull(vedlegg2.getSkattyternummer());
		assertNull(vedlegg2.getPostnummer());
		assertEquals(LAND_SE, vedlegg2.getLandkode());
	}

	@Test
	void shouldMapWithOnlyOneAddress() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_TRUE),
				createAdresseWithSingleAdress(),
				createHentPostdestinasjon());

		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());
	}

	private HentForsendelseResponse createHentForsendelseResponseTo(String mottakerType) {
		return HentForsendelseResponse.builder()
				.bestillingsId(BESTILLINGS_ID)
				.modus(MODUS)
				.mottaker(HentForsendelseResponse.Mottaker.builder()
						.mottakerId(MOTTAKER_ID)
						.mottakerNavn(MOTTAKER_NAVN)
						.mottakerType(mottakerType)
						.build())
				.dokumenter(Arrays.asList(HentForsendelseResponse.Dokument.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_HOVEDDOK)
								.dokumenttypeId(DOKUMENTTYPE_ID_HOVEDDOK)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.build(),
						HentForsendelseResponse.Dokument.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_VEDLEGG1)
								.dokumenttypeId(DOKUMENTTYPE_ID_VEDLEGG1)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build(),
						HentForsendelseResponse.Dokument.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_VEDLEGG2)
								.dokumenttypeId(DOKUMENTTYPE_ID_VEDLEGG2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build()))
				.build();
	}

	@Test
	void shouldAssertMottakerSkattyterToTrueWhenMottakerTypeErPersonOrOrganization(){
		assertTrue(bestillingMapper.isMottakerSkattyter(MOTTAKERTYPE_ORGANISASJON));
		assertTrue(bestillingMapper.isMottakerSkattyter(MOTTAKERTYPE_PERSON));
	}

	@Test
	void shouldAssertMottakerSkattyterToFalseWhenMottakerTypeErIkkePersonOrOrganization(){
		assertFalse(bestillingMapper.isMottakerSkattyter(MOTTAKERTYPE_UKJENT));
		assertFalse(bestillingMapper.isMottakerSkattyter(null));
	}

	private DokumenttypeInfo createDokumenttypeInfoTo(boolean tosidigPrint) {
		return DokumenttypeInfo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
				.tosidigprint(tosidigPrint)
				.build();
	}

	private DokumenttypeInfo createDokumenttypeInfoUtenKonvoluttvinduType(boolean tosidigPrint) {
		return DokumenttypeInfo.builder()
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
				.tosidigprint(tosidigPrint)
				.build();
	}

	private DokumenttypeInfo createDokumenttypeInfoToUtenSentralPrintDokumentType(boolean tosidigPrint) {
		return DokumenttypeInfo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.tosidigprint(tosidigPrint)
				.build();
	}

	private Adresse createAdresse(String landkode) {
		return Adresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(landkode)
				.build();
	}

	private Adresse createAdresseWithSingleAdress() {
		return Adresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(BestillingMapperTest.LAND_NO)
				.build();
	}

	private String createHentPostdestinasjon() {
		return new HentPostdestinasjonResponse(POST_DESTINASJON_INNLAND).postdestinasjon();
	}
}