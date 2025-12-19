package no.nav.dokdistsentralprint.qdist009.map;

import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.qdist009.BestillingMapper;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.dokdistsentralprint.TestData.ADRESSELINJE_1;
import static no.nav.dokdistsentralprint.TestData.ADRESSELINJE_2;
import static no.nav.dokdistsentralprint.TestData.ADRESSELINJE_3;
import static no.nav.dokdistsentralprint.TestData.ADRESSELINJE_LANG;
import static no.nav.dokdistsentralprint.TestData.BESTILLINGS_ID;
import static no.nav.dokdistsentralprint.TestData.CDATA_MOTTAKER_NAVN;
import static no.nav.dokdistsentralprint.TestData.KONVOLUTTVINDU_TYPE;
import static no.nav.dokdistsentralprint.TestData.LAND_NO;
import static no.nav.dokdistsentralprint.TestData.LAND_SE;
import static no.nav.dokdistsentralprint.TestData.LAND_SE_NAVN;
import static no.nav.dokdistsentralprint.TestData.MODUS;
import static no.nav.dokdistsentralprint.TestData.MOTTAKERTYPE_ORGANISASJON;
import static no.nav.dokdistsentralprint.TestData.MOTTAKERTYPE_PERSON;
import static no.nav.dokdistsentralprint.TestData.MOTTAKERTYPE_UKJENT;
import static no.nav.dokdistsentralprint.TestData.MOTTAKER_ID;
import static no.nav.dokdistsentralprint.TestData.MOTTAKER_NAVN;
import static no.nav.dokdistsentralprint.TestData.MOTTAKER_NAVN_LANG;
import static no.nav.dokdistsentralprint.TestData.NAV_STANDARD;
import static no.nav.dokdistsentralprint.TestData.OBJEKT_REFERANSE_HOVEDDOK;
import static no.nav.dokdistsentralprint.TestData.OBJEKT_REFERANSE_VEDLEGG1;
import static no.nav.dokdistsentralprint.TestData.OBJEKT_REFERANSE_VEDLEGG2;
import static no.nav.dokdistsentralprint.TestData.PORTOKLASSE;
import static no.nav.dokdistsentralprint.TestData.POSTNUMMER;
import static no.nav.dokdistsentralprint.TestData.POSTSTED;
import static no.nav.dokdistsentralprint.TestData.POST_DESTINASJON_INNLAND;
import static no.nav.dokdistsentralprint.TestData.SENTRALPRINT_DOKTYPE;
import static no.nav.dokdistsentralprint.TestData.TOSIDIG_PRINT_FALSE;
import static no.nav.dokdistsentralprint.TestData.TOSIDIG_PRINT_TRUE;
import static no.nav.dokdistsentralprint.TestData.createAdresse;
import static no.nav.dokdistsentralprint.TestData.createAdresseWithSingleAdress;
import static no.nav.dokdistsentralprint.TestData.createDokumenttypeInfoTo;
import static no.nav.dokdistsentralprint.TestData.createDokumenttypeInfoToUtenSentralPrintDokumentType;
import static no.nav.dokdistsentralprint.TestData.createDokumenttypeInfoUtenKonvoluttvinduType;
import static no.nav.dokdistsentralprint.TestData.createHentForsendelseResponseTo;
import static no.nav.dokdistsentralprint.TestData.createHentForsendelseResponseToBuilder;
import static no.nav.dokdistsentralprint.TestData.createHentPostdestinasjon;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.KUNDE_ID_NAV_IKT;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.PRINT;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.USORTERT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestillingMapperTest {

	static final String MOTTAKER_NAVN_LANG_FORVENTET = "Alejandro González Hernández de la Rosa Martínez de la Cruz y Rodríguez del Valle García Sánchez de los Santos Pérez López Ra...";
	static final String ADRESSELINJE_LANG_FORVENTET = "Calle Real de la Esperanza, 23, Edificio del Ministerio de Asuntos Internos y Externos, Departamento de Gestión de Recursos y...";

	private final BestillingMapper bestillingMapper = new BestillingMapper();

	@Test
	void shouldMap() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresse(LAND_NO), MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_TRUE),
				createHentPostdestinasjon()
		);

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

		Dokument hoveddokument = bestilling.getMailpiece().getDokument().getFirst();
		assertEquals(SENTRALPRINT_DOKTYPE, hoveddokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hoveddokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hoveddokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hoveddokument.getSkattyternummer());
		assertNull(hoveddokument.getLandkode());
		assertEquals(POSTNUMMER, hoveddokument.getPostnummer());

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
	void shouldMapDefaultValueWhenSentralPrintDokumentTypeIkkeSettPaaDokumenttypeInfo() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresse(LAND_NO), MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoToUtenSentralPrintDokumentType(TOSIDIG_PRINT_TRUE),
				createHentPostdestinasjon()
		);

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

		Dokument hoveddokument = bestilling.getMailpiece().getDokument().getFirst();
		assertEquals(NAV_STANDARD, hoveddokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hoveddokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hoveddokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hoveddokument.getSkattyternummer());
		assertNull(hoveddokument.getLandkode());
		assertEquals(POSTNUMMER, hoveddokument.getPostnummer());

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
	void shouldMapKonvoluttTypeToXWhenKonvoluttvinduTypeIkkeSettPaaDokumenttypeInfo() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresse(LAND_NO), MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoUtenKonvoluttvinduType(TOSIDIG_PRINT_TRUE),
				createHentPostdestinasjon()
		);

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

		Dokument hoveddokument = bestilling.getMailpiece().getDokument().getFirst();
		assertEquals(SENTRALPRINT_DOKTYPE, hoveddokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hoveddokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hoveddokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hoveddokument.getSkattyternummer());
		assertNull(hoveddokument.getLandkode());
		assertEquals(POSTNUMMER, hoveddokument.getPostnummer());

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
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresse(LAND_NO), MOTTAKERTYPE_ORGANISASJON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createHentPostdestinasjon()
		);

		assertEquals(PORTOKLASSE + "_" + KONVOLUTTVINDU_TYPE + "_S", bestilling.getBestillingsInfo().getKanal().getBehandling());
	}

	@Test
	void shouldMapBestillingWithUtenlandsLandkode() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresse(LAND_SE), MOTTAKERTYPE_ORGANISASJON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createHentPostdestinasjon()
		);

		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
					 ADRESSELINJE_1 + "\r" +
					 ADRESSELINJE_2 + "\r" +
					 ADRESSELINJE_3 + "\r" +
					 POSTNUMMER + " " + POSTSTED + "\r" + LAND_SE_NAVN + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertEquals(LAND_SE, bestilling.getMailpiece().getLandkode());
		assertNull(bestilling.getMailpiece().getPostnummer());

		Dokument hoveddokument = bestilling.getMailpiece().getDokument().getFirst();
		assertEquals(SENTRALPRINT_DOKTYPE, hoveddokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hoveddokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hoveddokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hoveddokument.getSkattyternummer());
		assertEquals(LAND_SE, hoveddokument.getLandkode());
		assertNull(hoveddokument.getPostnummer());

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
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresse(LAND_SE), MOTTAKERTYPE_UKJENT),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createHentPostdestinasjon()
		);

		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
					 ADRESSELINJE_1 + "\r" +
					 ADRESSELINJE_2 + "\r" +
					 ADRESSELINJE_3 + "\r" +
					 POSTNUMMER + " " + POSTSTED + "\r" + LAND_SE_NAVN + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertEquals(LAND_SE, bestilling.getMailpiece().getLandkode());
		assertNull(bestilling.getMailpiece().getPostnummer());

		Dokument hoveddokument = bestilling.getMailpiece().getDokument().getFirst();
		assertEquals(SENTRALPRINT_DOKTYPE, hoveddokument.getDokumentType());
		assertEquals(CDATA_MOTTAKER_NAVN, hoveddokument.getNavn());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hoveddokument.getDokumentId());
		assertNull(hoveddokument.getSkattyternummer());
		assertEquals(LAND_SE, hoveddokument.getLandkode());
		assertNull(hoveddokument.getPostnummer());

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
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(
				createAdresseWithSingleAdress(), MOTTAKERTYPE_PERSON),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_TRUE),
				createHentPostdestinasjon()
		);

		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
					 ADRESSELINJE_1 + "\r" +
					 POSTNUMMER + " " + POSTSTED + "\r" + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());
	}

	@Test
	void shouldTruncateMottakerAndAdresseLinjeTo128Chars() {
		InternForsendelse.InternForsendelseBuilder hentForsendelseResponseToBuilder = createHentForsendelseResponseToBuilder(InternForsendelse.Postadresse.builder()
				.adresselinje1(ADRESSELINJE_LANG)
				.adresselinje2(ADRESSELINJE_LANG)
				.adresselinje3(ADRESSELINJE_LANG)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND_NO)
				.build(), MOTTAKERTYPE_PERSON)
				.mottaker(InternForsendelse.Mottaker.builder()
						.mottakerId(MOTTAKER_ID)
						.mottakerNavn(MOTTAKER_NAVN_LANG)
						.mottakerType(MOTTAKERTYPE_PERSON)
						.build());
		Bestilling bestilling = bestillingMapper.createBestilling(hentForsendelseResponseToBuilder.build(),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_TRUE),
				createHentPostdestinasjon());

		assertEquals("<![CDATA[" + MOTTAKER_NAVN_LANG_FORVENTET + "\r" +
					 ADRESSELINJE_LANG_FORVENTET + "\r" +
					 ADRESSELINJE_LANG_FORVENTET + "\r" +
					 ADRESSELINJE_LANG_FORVENTET + "\r" +
					 POSTNUMMER + " " + POSTSTED + "\r" + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());
	}

	@Test
	void shouldAssertMottakerSkattyterToTrueWhenMottakerTypeErPersonOrOrganization() {
		assertTrue(bestillingMapper.isMottakerSkattyter(MOTTAKERTYPE_ORGANISASJON));
		assertTrue(bestillingMapper.isMottakerSkattyter(MOTTAKERTYPE_PERSON));
	}

	@Test
	void shouldAssertMottakerSkattyterToFalseWhenMottakerTypeErIkkePersonOrOrganization() {
		assertFalse(bestillingMapper.isMottakerSkattyter(MOTTAKERTYPE_UKJENT));
		assertFalse(bestillingMapper.isMottakerSkattyter(null));
	}
}