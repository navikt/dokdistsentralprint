package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.consumer.rdist001.ArkivSystemCode;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentPostdestinasjonResponse;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumenttypeInfo;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;

import java.util.Arrays;

public class TestData {

	public static final Long FORSENDELSE_ID = 111111111L;
	public static final String BESTILLINGS_ID = "bestillingsId";
	public static final String TEMA = "DAG";
	public static final String MODUS = "modus";
	public static final String FORSENDELSE_STATUS = "KLAR_FOR_DIST";
	public static final String MOTTAKER_NAVN = "mottakerNavn";
	public static final String MOTTAKER_NAVN_FOR_LANG = "Dette mottakernavnet er 131 bokstaver lang, men bare de 128 første skal være med i adresselinjen.. tegn nr 129 kommer bak kolon:her";
	public static final String MOTTAKER_NAVN_FOR_LANG_128_TEGN = "Dette mottakernavnet er 131 bokstaver lang, men bare de 128 første skal være med i adresselinjen.. tegn nr 129 kommer bak kolon:";
	public static final String MOTTAKER_ID = "mottakerId";
	public static final String ADRESSELINJE_1 = "adresselinje1";
	public static final String ADRESSELINJE_1_FOR_LANG = "Denne adresselinje 1 er 131 bokstaver lang, men bare de 128 første skal være med i adresselinjen.. tegn nr 129 kommer bak kolon:her";
	public static final String ADRESSELINJE_1_FOR_LANG_128_TEGN = "Denne adresselinje 1 er 131 bokstaver lang, men bare de 128 første skal være med i adresselinjen.. tegn nr 129 kommer bak kolon:";
	public static final String ADRESSELINJE_2 = "adresselinje2";
	public static final String ADRESSELINJE_3 = "adresselinje3";
	public static final String POSTNUMMER = "postnummer";
	public static final String POSTSTED = "poststed";
	public static final String LAND_NO = "NO";
	public static final String LAND_SE = "SE";
	public static final String LAND_SE_NAVN = "SVERIGE";
	public static final String OBJEKT_REFERANSE_HOVEDDOK = "objektreferanseHoveddok";
	public static final String DOKUMENTTYPE_ID_HOVEDDOK = "dokumenttypeIdHoveddok";
	public static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";

	public static final String OBJEKT_REFERANSE_VEDLEGG1 = "objektreferanseVedlegg1";
	public static final String DOKUMENTTYPE_ID_VEDLEGG1 = "dokumenttypeIdVedlegg1";
	public static final String OBJEKT_REFERANSE_VEDLEGG2 = "objektreferanseVedlegg2";
	public static final String DOKUMENTTYPE_ID_VEDLEGG2 = "dokumenttypeIdVedlegg2";
	public static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";

	public static final String KONVOLUTTVINDU_TYPE = "konvoluttvinduType";
	public static final String PORTOKLASSE = "portoklasse";
	public static final String SENTRALPRINT_DOKTYPE = "sentPrintDokType";
	public static final String POST_DESTINASJON_INNLAND = "INNLAND";
	public static final boolean TOSIDIG_PRINT_TRUE = true;
	public static final boolean TOSIDIG_PRINT_FALSE = false;
	public static final String MOTTAKERTYPE_PERSON = "PERSON";
	public static final String MOTTAKERTYPE_ORGANISASJON = "ORGANISASJON";
	public static final String MOTTAKERTYPE_UKJENT = "UKJENT";
	public static final String CDATA_MOTTAKER_NAVN = "<![CDATA[" + MOTTAKER_NAVN + "]]>";
	public static final String NAV_STANDARD = "NAV_STANDARD";
	public static final String JOURNALPOST_ID = "123456789";


	public static InternForsendelse createHentForsendelseResponseTo(InternForsendelse.Postadresse postadresse, String mottakerType) {
		return InternForsendelse.builder()
				.bestillingsId(BESTILLINGS_ID)
				.modus(MODUS)
				.postadresse(postadresse)
				.mottaker(InternForsendelse.Mottaker.builder()
						.mottakerId(MOTTAKER_ID)
						.mottakerNavn(MOTTAKER_NAVN)
						.mottakerType(mottakerType)
						.build())
				.dokumenter(Arrays.asList(InternForsendelse.Dokument.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_HOVEDDOK)
								.dokumenttypeId(DOKUMENTTYPE_ID_HOVEDDOK)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.build(),
						InternForsendelse.Dokument.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_VEDLEGG1)
								.dokumenttypeId(DOKUMENTTYPE_ID_VEDLEGG1)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build(),
						InternForsendelse.Dokument.builder()
								.dokumentObjektReferanse(OBJEKT_REFERANSE_VEDLEGG2)
								.dokumenttypeId(DOKUMENTTYPE_ID_VEDLEGG2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.build()))
				.build();
	}

	public static HentForsendelseResponse createHentForsendelseResponse() {
		return HentForsendelseResponse.builder()
				.forsendelseId(FORSENDELSE_ID)
				.bestillingsId(BESTILLINGS_ID)
				.tema(TEMA)
				.modus(MODUS)
				.forsendelseStatus(FORSENDELSE_STATUS)
				.postadresse(createPostadresse())
				.arkivInformasjon(createArkivInformasjon())
				.mottaker(HentForsendelseResponse.Mottaker.builder()
						.mottakerId(MOTTAKER_ID)
						.mottakerNavn(MOTTAKER_NAVN)
						.mottakerType(MOTTAKERTYPE_PERSON)
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


	public static DokumenttypeInfo createDokumenttypeInfoTo(boolean tosidigPrint) {
		return DokumenttypeInfo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
				.tosidigprint(tosidigPrint)
				.build();
	}

	public static DokumenttypeInfo createDokumenttypeInfoUtenKonvoluttvinduType(boolean tosidigPrint) {
		return DokumenttypeInfo.builder()
				.portoklasse(PORTOKLASSE)
				.sentralPrintDokumentType(SENTRALPRINT_DOKTYPE)
				.tosidigprint(tosidigPrint)
				.build();
	}

	public static DokumenttypeInfo createDokumenttypeInfoToUtenSentralPrintDokumentType(boolean tosidigPrint) {
		return DokumenttypeInfo.builder()
				.konvoluttvinduType(KONVOLUTTVINDU_TYPE)
				.portoklasse(PORTOKLASSE)
				.tosidigprint(tosidigPrint)
				.build();
	}

	public static InternForsendelse.Postadresse createAdresse(String landkode) {
		return InternForsendelse.Postadresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(landkode)
				.build();
	}

	public static InternForsendelse.Postadresse createAdresseWithSingleAdress() {
		return InternForsendelse.Postadresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND_NO)
				.build();
	}


	public static HentForsendelseResponse.Postadresse createPostadresse() {
		return HentForsendelseResponse.Postadresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND_NO)
				.build();
	}

	public static HentForsendelseResponse.ArkivInformasjon createArkivInformasjon() {
		return HentForsendelseResponse.ArkivInformasjon.builder()
				.arkivId(JOURNALPOST_ID)
				.arkivSystem(ArkivSystemCode.JOARK)
				.build();
	}

	public static String createHentPostdestinasjon() {
		return new HentPostdestinasjonResponse(POST_DESTINASJON_INNLAND).postdestinasjon();
	}
}
