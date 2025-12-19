package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import org.junit.jupiter.api.Test;

import static no.nav.dokdistsentralprint.TestData.ADRESSELINJE_1;
import static no.nav.dokdistsentralprint.TestData.BESTILLINGS_ID;
import static no.nav.dokdistsentralprint.TestData.FORSENDELSE_ID;
import static no.nav.dokdistsentralprint.TestData.FORSENDELSE_STATUS;
import static no.nav.dokdistsentralprint.TestData.JOURNALPOST_ID;
import static no.nav.dokdistsentralprint.TestData.LAND_NO;
import static no.nav.dokdistsentralprint.TestData.MOTTAKERTYPE_PERSON;
import static no.nav.dokdistsentralprint.TestData.MOTTAKER_ID;
import static no.nav.dokdistsentralprint.TestData.MOTTAKER_NAVN;
import static no.nav.dokdistsentralprint.TestData.POSTNUMMER;
import static no.nav.dokdistsentralprint.TestData.POSTSTED;
import static no.nav.dokdistsentralprint.TestData.TEMA;
import static no.nav.dokdistsentralprint.TestData.createHentForsendelseResponse;
import static no.nav.dokdistsentralprint.consumer.rdist001.ArkivSystemCode.JOARK;
import static org.assertj.core.api.Assertions.assertThat;

class ForsendelseMapperTest {

	final ForsendelseMapper forsendelseMapper = new ForsendelseMapper();

	@Test
	public void mapHentForsendelseToInternForsendelse() {
		InternForsendelse internForsendelse = forsendelseMapper.mapForsendelse(createHentForsendelseResponse());

		assertThat(internForsendelse.getForsendelseId()).isEqualTo(FORSENDELSE_ID);
		assertThat(internForsendelse.getBestillingsId()).isEqualTo(BESTILLINGS_ID);
		assertThat(internForsendelse.getTema()).isEqualTo(TEMA);
		assertThat(internForsendelse.getForsendelseStatus()).isEqualTo(FORSENDELSE_STATUS);

		assertPostadresse(internForsendelse.getPostadresse());
		assertMottaker(internForsendelse.getMottaker());
		assertDokument(internForsendelse);

		assertThat(internForsendelse.getArkivInformasjon().getArkivId()).isEqualTo(JOURNALPOST_ID);
		assertThat(internForsendelse.getArkivInformasjon().getArkivSystem()).isEqualTo(JOARK);
	}

	private void assertMottaker(InternForsendelse.Mottaker mottaker) {
		assertThat(mottaker.getMottakerId()).isEqualTo(MOTTAKER_ID);
		assertThat(mottaker.getMottakerNavn()).isEqualTo(MOTTAKER_NAVN);
		assertThat(mottaker.getMottakerType()).isEqualTo(MOTTAKERTYPE_PERSON);
	}

	private void assertPostadresse(InternForsendelse.Postadresse postadresse) {
		assertThat(postadresse.getAdresselinje1()).isEqualTo(ADRESSELINJE_1);
		assertThat(postadresse.getAdresselinje2()).isNull();
		assertThat(postadresse.getAdresselinje3()).isNull();
		assertThat(postadresse.getPostnummer()).isEqualTo(POSTNUMMER);
		assertThat(postadresse.getPoststed()).isEqualTo(POSTSTED);
		assertThat(postadresse.getLandkode()).isEqualTo(LAND_NO);
	}

	private void assertDokument(InternForsendelse internForsendelse) {
		internForsendelse.getDokumenter().forEach(dokument ->
				assertThat(dokument)
						.hasFieldOrPropertyWithValue("dokumentObjektReferanse", dokument.getDokumentObjektReferanse())
						.hasFieldOrPropertyWithValue("tilknyttetSom", dokument.getTilknyttetSom())
						.hasFieldOrPropertyWithValue("dokumenttypeId", dokument.getDokumenttypeId())
		);
	}
}