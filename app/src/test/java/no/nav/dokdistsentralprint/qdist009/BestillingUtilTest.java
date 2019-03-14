package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponseTo;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
class BestillingUtilTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String MODUS = "modus";
	private static final String MOTTAKER_NAVN = "mottakerNavn";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND = "land";
	private static final String OBJEKT_REFERANSE_HOVEDDOK = "objektreferanseHoveddok";
	private static final String DOKUMENTTYPE_ID_HOVEDDOK = "dokumenttypeIdHoveddok";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String KONVOLUTTVINDU_TYPE = "konvoluttvinduType";
	private static final String PORTOKLASSE = "portoklasse";
	private static final String SENTRALPRINT_DOKTYPE = "sentPrintDokType";


	@Test
	public void shouldMarshal() throws Throwable {
		BestillingUtil bestillingUtil = new BestillingUtil();
		Bestilling bestilling = bestillingUtil.createBestilling(createHentForsendelseResponseTo(), createDokumenttypeInfoTo(), createAdresse());
//		String outputXml = bestillingUtil.marshalBestillingToXmlString(bestilling);
		File file = bestillingUtil.marshalBestillingToXmlFile(bestilling);
		bestillingUtil.zipFile(file);
	}

	private Adresse createAdresse() {
		return Adresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND)
				.build();
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
						.build()))
				.build();
	}

	private DokumenttypeInfoTo createDokumenttypeInfoTo() {
		return DokumenttypeInfoTo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
				.tosidigprint(true)
				.build();
	}

}