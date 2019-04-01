package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.KUNDE_ID_NAV_IKT;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.PRINT;
import static no.nav.dokdistsentralprint.qdist009.BestillingMapper.USORTERT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.rdist001.HentPostDestinasjonResponseTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.printoppdrag.Dokument;
import no.nav.dokdistsentralprint.qdist009.domain.Adresse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
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
	private static final String LAND_US = "US";
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

	private final BestillingMapper bestillingMapper = new BestillingMapper();


	@Test
	public void shouldMap() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_TRUE),
				createAdresse(LAND_NO),
				createHentPostDestinasjonresponseTo());

		assertEquals(MODUS, bestilling.getBestillingsInfo().getModus());
		assertEquals(KUNDE_ID_NAV_IKT, bestilling.getBestillingsInfo().getKundeId());
		assertEquals(BESTILLINGS_ID, bestilling.getBestillingsInfo().getBestillingsId());
		assertNotNull(LocalDate.now().toString(), bestilling.getBestillingsInfo().getKundeOpprettet());

		assertNotNull(USORTERT, bestilling.getBestillingsInfo().getDokumentInfo().getSorteringsfelt());
		assertEquals(POST_DESTINASJON_INNLAND, bestilling.getBestillingsInfo().getDokumentInfo().getDestinasjon());
		assertEquals(PRINT, bestilling.getBestillingsInfo().getKanal().getType()); // type set by constant

		assertEquals(PORTOKLASSE + "_" + KONVOLUTTVINDU_TYPE + "_D", bestilling.getBestillingsInfo().getKanal().getBehandling());
		assertEquals(BESTILLINGS_ID, bestilling.getMailpiece().getMailpieceId());

		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + LAND_NO + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertEquals(LAND_NO, bestilling.getMailpiece().getLandkode());
		assertEquals(POSTNUMMER, bestilling.getMailpiece().getPostnummer());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().iterator().next();

		assertEquals(SENTRALPRINT_DOKTYPE, hovedDokument.getDokumentType());
		assertEquals(OBJEKT_REFERANSE_HOVEDDOK, hovedDokument.getDokumentId());
		assertEquals(MOTTAKER_ID, hovedDokument.getSkattyternummer());
		assertEquals(LAND_NO, hovedDokument.getLandkode());
		assertEquals(POSTNUMMER, hovedDokument.getPostnummer());
	}

	@Test
	public void shouldMapBestillingWithTosidigPrintFalse() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createAdresse(LAND_NO),
				createHentPostDestinasjonresponseTo());

		assertEquals(PORTOKLASSE + "_" + KONVOLUTTVINDU_TYPE + "_S", bestilling.getBestillingsInfo().getKanal().getBehandling());
	}


	@Test
	public void shouldMapBestillingWithUtenlandsLandkode() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(),
				createDokumenttypeInfoTo(TOSIDIG_PRINT_FALSE),
				createAdresse(LAND_US),
				createHentPostDestinasjonresponseTo());

		Dokument hovedDokument = bestilling.getMailpiece().getDokument().iterator().next();

		assertEquals("<![CDATA[" + MOTTAKER_NAVN + "\r" +
						ADRESSELINJE_1 + "\r" +
						ADRESSELINJE_2 + "\r" +
						ADRESSELINJE_3 + "\r" +
						POSTNUMMER + " " + POSTSTED + "\r" + LAND_US + "]]>",
				bestilling.getMailpiece().getRessurs().getAdresse());

		assertNull(bestilling.getMailpiece().getPostnummer());
		assertNull(bestilling.getMailpiece().getLandkode());
		assertNull(hovedDokument.getLandkode());
	}

	private HentForsendelseResponseTo createHentForsendelseResponseTo() {
		return HentForsendelseResponseTo.builder()
				.bestillingsId(BESTILLINGS_ID)
				.modus(MODUS)
				.mottaker(HentForsendelseResponseTo.MottakerTo.builder()
						.mottakerId(MOTTAKER_ID)
						.mottakerNavn(MOTTAKER_NAVN)
						.build())
				.dokumenter(Arrays.asList(HentForsendelseResponseTo.DokumentTo.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_HOVEDDOK)
								.dokumenttypeId(DOKUMENTTYPE_ID_HOVEDDOK)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.build(),
						HentForsendelseResponseTo.DokumentTo.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_VEDLEGG1)
								.dokumenttypeId(DOKUMENTTYPE_ID_VEDLEGG1)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build(),
						HentForsendelseResponseTo.DokumentTo.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_VEDLEGG2)
								.dokumenttypeId(DOKUMENTTYPE_ID_VEDLEGG2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build()))
				.build();
	}

	private DokumenttypeInfoTo createDokumenttypeInfoTo(boolean tosidigPrint) {
		return DokumenttypeInfoTo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
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

	private HentPostDestinasjonResponseTo createHentPostDestinasjonresponseTo() {
		return HentPostDestinasjonResponseTo.builder().postDestinasjon(POST_DESTINASJON_INNLAND).build();
	}
}