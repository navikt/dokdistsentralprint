package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.ArkivInformasjon;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.Dokument;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.Mottaker;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.Postadresse;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.substring;

@Component
public class ForsendelseMapper {

	public InternForsendelse mapForsendelse(HentForsendelseResponse hentForsendelseResponse) {
		return InternForsendelse.builder()
				.forsendelseId(hentForsendelseResponse.getForsendelseId())
				.bestillingsId(hentForsendelseResponse.getBestillingsId())
				.originalBestillingsId(hentForsendelseResponse.getOriginalBestillingsId())
				.forsendelseStatus(hentForsendelseResponse.getForsendelseStatus())
				.tema(hentForsendelseResponse.getTema())
				.modus(hentForsendelseResponse.getModus())
				.postadresse(mapPostadresse(hentForsendelseResponse.getPostadresse()).orElse(null))
				.arkivInformasjon(mapArkivInformasjon(hentForsendelseResponse.getArkivInformasjon()).orElse(null))
				.mottaker(mapMottaker(hentForsendelseResponse.getMottaker()))
				.dokumenter(hentForsendelseResponse.getDokumenter().stream().map(dokument -> mapDokument(dokument)).toList())
				.build();
	}

	private Optional<Postadresse> mapPostadresse(HentForsendelseResponse.Postadresse adresse) {
		return adresse == null ? Optional.empty() : ofNullable(Postadresse.builder()
				.adresselinje1(substring(adresse.getAdresselinje1(), 0, 128))
				.adresselinje2(substring(adresse.getAdresselinje2(),0,128))
				.adresselinje3(substring(adresse.getAdresselinje3(),0,128))
				.postnummer(adresse.getPostnummer())
				.poststed(adresse.getPoststed())
				.landkode(adresse.getLandkode())
				.build());
	}

	private Optional<ArkivInformasjon> mapArkivInformasjon(HentForsendelseResponse.ArkivInformasjon arkivInformasjon) {
		return arkivInformasjon == null ? Optional.empty() : ofNullable(ArkivInformasjon.builder()
				.arkivId(arkivInformasjon.getArkivId())
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.build());
	}

	private Mottaker mapMottaker(HentForsendelseResponse.Mottaker mottaker) {
		return Mottaker.builder()
				.mottakerId(mottaker.getMottakerId())
				.mottakerNavn(substring(mottaker.getMottakerNavn(),0,128))
				.mottakerType(mottaker.getMottakerType())
				.build();
	}

	private Dokument mapDokument(HentForsendelseResponse.Dokument dokument) {
		return dokument == null ? null : Dokument.builder()
				.dokumenttypeId(dokument.getDokumenttypeId())
				.dokumentObjektReferanse(dokument.getDokumentObjektReferanse())
				.tilknyttetSom(dokument.getTilknyttetSom())
				.build();
	}
}
