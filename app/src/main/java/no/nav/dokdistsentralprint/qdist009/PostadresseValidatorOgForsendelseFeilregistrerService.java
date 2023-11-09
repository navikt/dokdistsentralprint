package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistsentralprint.consumer.rdist001.FeilregistrerForsendelseRequest;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterPostadresseRequest;
import no.nav.dokdistsentralprint.consumer.regoppslag.Regoppslag;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.qdist009.domain.DistribuerForsendelseTilSentralPrintTo;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class PostadresseValidatorOgForsendelseFeilregistrerService {

	public static final String UKJENT_LANDKODE = "???";
	public static final String XX_LANDKODE = "XX";
	private static final String FORSENDELSE_FEIL_TYPE = "MELDINGSFEIL";
	private static final String FEIL_MELDING_DETALJER = "Manglende adresse";
	private final Regoppslag regoppslag;
	private final AdministrerForsendelseConsumer administrerForsendelse;

	public PostadresseValidatorOgForsendelseFeilregistrerService(Regoppslag regoppslag,
																 AdministrerForsendelseConsumer administrerForsendelse) {
		this.regoppslag = regoppslag;
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public HentForsendelseResponse hentForsendelse(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo) {
		final String forsendelseId = distribuerForsendelseTilSentralPrintTo.getForsendelseId();
		log.info("qdist009 har mottatt bestilling til print med forsendelseId={}", forsendelseId);

		HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);
		HentForsendelseResponse.Postadresse adresse = getAdresse(hentForsendelseResponse);
		if (adresse == null) {
			administrerForsendelse.feilregistrerForsendelse(mapFeilregistrerForsendelse(hentForsendelseResponse));
			return hentForsendelseResponse;
		}

		if (hentForsendelseResponse.getPostadresse() == null) {
			HentForsendelseResponse.Postadresse postadresse = HentForsendelseResponse.Postadresse.builder()
					.adresselinje1(adresse.getAdresselinje1())
					.adresselinje2(adresse.getAdresselinje2())
					.adresselinje3(adresse.getAdresselinje3())
					.postnummer(adresse.getPostnummer())
					.poststed(adresse.getPoststed())
					.landkode(adresse.getLandkode())
					.build();
			hentForsendelseResponse.setPostadresse(postadresse);
		}

		return hentForsendelseResponse;
	}

	public String hentPostdestinasjon(HentForsendelseResponse.Postadresse adresse) {
		return administrerForsendelse.hentPostdestinasjon(adresse.getLandkode());
	}

	private HentForsendelseResponse.Postadresse getAdresse(HentForsendelseResponse hentForsendelseResponse) {
		final HentForsendelseResponse.Postadresse adresseDokdist = hentForsendelseResponse.getPostadresse();
		if (adresseDokdist == null) {
			HentForsendelseResponse.Postadresse pdlPostadresse = getAdresseFromRegoppslag(hentForsendelseResponse);
			oppdaterPostadresse(hentForsendelseResponse.getForsendelseId(), pdlPostadresse);
			return pdlPostadresse;
		} else {
			return HentForsendelseResponse.Postadresse.builder()
					.adresselinje1(adresseDokdist.getAdresselinje1())
					.adresselinje2(adresseDokdist.getAdresselinje2())
					.adresselinje3(adresseDokdist.getAdresselinje3())
					.landkode(mapLandkode(adresseDokdist.getLandkode()))
					.postnummer(adresseDokdist.getPostnummer())
					.poststed(adresseDokdist.getPoststed())
					.build();
		}
	}

	private void oppdaterPostadresse(Long forsendelseId, HentForsendelseResponse.Postadresse postadresse) {
		if (postadresse != null) {
			administrerForsendelse.oppdaterPostadresse(mapOppdaterPostadresse(forsendelseId, postadresse));
		}
	}

	private HentForsendelseResponse.Postadresse getAdresseFromRegoppslag(HentForsendelseResponse hentForsendelseResponse) {
		AdresseTo adresseTo = regoppslag.treg002HentAdresse(HentAdresseRequestTo.builder()
				.identifikator(hentForsendelseResponse.getMottaker().getMottakerId())
				.type(hentForsendelseResponse.getMottaker().getMottakerType())
				.tema(hentForsendelseResponse.getTema())
				.build());

		return adresseTo == null ? null :
				HentForsendelseResponse.Postadresse.builder()
						.adresselinje1(adresseTo.getAdresselinje1())
						.adresselinje2(adresseTo.getAdresselinje2())
						.adresselinje3(adresseTo.getAdresselinje3())
						.landkode(mapLandkode(adresseTo.getLandkode()))
						.postnummer(adresseTo.getPostnummer())
						.poststed(adresseTo.getPoststed())
						.build();
	}

	private String mapLandkode(String landkode) {
		return isBlank(landkode) || UKJENT_LANDKODE.equals(landkode) ? XX_LANDKODE : landkode;
	}

	private OppdaterPostadresseRequest mapOppdaterPostadresse(Long forsendelseId, HentForsendelseResponse.Postadresse adresse) {
		return OppdaterPostadresseRequest.builder()
				.forsendelseId(forsendelseId)
				.adresselinje1(adresse.getAdresselinje1())
				.adresselinje2(adresse.getAdresselinje2())
				.adresselinje3(adresse.getAdresselinje3())
				.postnummer(adresse.getPostnummer())
				.poststed(adresse.getPoststed())
				.landkode(adresse.getLandkode())
				.build();
	}

	private FeilregistrerForsendelseRequest mapFeilregistrerForsendelse(HentForsendelseResponse hentForsendelseResponse) {
		return FeilregistrerForsendelseRequest.builder()
				.forsendelseId(hentForsendelseResponse.getForsendelseId())
				.feilTypeCode(FORSENDELSE_FEIL_TYPE)
				.detaljer(FEIL_MELDING_DETALJER)
				.tidspunkt(LocalDateTime.now())
				.build();
	}
}
