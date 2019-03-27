package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.rdist001.HentPostDestinasjonResponseTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.qdist009.domain.Adresse;
import org.junit.jupiter.api.Test;

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

	private final BestillingMapper bestillingMapper = new BestillingMapper();


	@Test
	public void shouldMap() {
		Bestilling bestilling = bestillingMapper.createBestilling(createHentForsendelseResponseTo(), createDokumenttypeInfoTo(), createAdresse(), createHentPostDestinasjonresponseTo());

		//Todo Assert verdier i Bestilling. Se lb!
	}

	//TODO: Legg til tester slik at man dekker alle kodestier i mapperen. Se lb!


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

	private DokumenttypeInfoTo createDokumenttypeInfoTo() {
		return DokumenttypeInfoTo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
				.tosidigprint(TOSIDIG_PRINT_TRUE)
				.build();
	}

	private Adresse createAdresse() {
		return Adresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND_NO)
				.build();
	}

	private HentPostDestinasjonResponseTo createHentPostDestinasjonresponseTo() {
		return HentPostDestinasjonResponseTo.builder().postDestinasjon(POST_DESTINASJON_INNLAND).build();
	}
}