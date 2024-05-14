package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistsentralprint.consumer.rdist001.FeilregistrerForsendelseRequest;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterPostadresseRequest;
import no.nav.dokdistsentralprint.consumer.regoppslag.RegoppslagRestConsumer;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.qdist009.domain.DistribuerForsendelseTilSentralPrintTo;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse.Postadresse;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistsentralprint.qdist009.util.Qdist009FunctionalUtils.validateForsendelsestatus;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class PostadresseValidatorOgForsendelseFeilregistrerService {

	public static final String UKJENT_LANDKODE = "???";
	public static final String XX_LANDKODE = "XX";
	private static final String FORSENDELSE_FEIL_TYPE = "MELDINGSFEIL";
	private static final String FEIL_MELDING_DETALJER = "Manglende adresse";
	private final RegoppslagRestConsumer regoppslagRestConsumer;
	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final ForsendelseMapper forsendelseMapper;

	public PostadresseValidatorOgForsendelseFeilregistrerService(RegoppslagRestConsumer regoppslagRestConsumer,
																 ForsendelseMapper forsendelseMapper,
																 AdministrerForsendelseConsumer administrerForsendelse) {
		this.regoppslagRestConsumer = regoppslagRestConsumer;
		this.administrerForsendelse = administrerForsendelse;
		this.forsendelseMapper = forsendelseMapper;
	}

	@Handler
	public InternForsendelse hentForsendelse(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo, Exchange exchange) {
		final String forsendelseId = distribuerForsendelseTilSentralPrintTo.getForsendelseId();
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(forsendelseId);
		validateForsendelsestatus(hentForsendelseResponse.getForsendelseStatus());

		final String bestillingsId = hentForsendelseResponse.getBestillingsId();
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);

		log.info("qdist009 har mottatt bestilling til print med forsendelseId={}, bestillingsId={}", forsendelseId, bestillingsId);


		if (hentForsendelseResponse.getPostadresse() != null) {
			return forsendelseMapper.mapForsendelse(hentForsendelseResponse);
		}

		return mapPostadresseAndForsendelse(hentForsendelseResponse);
	}

	public String hentPostdestinasjon(Postadresse adresse) {
		return administrerForsendelse.hentPostdestinasjon(adresse.getLandkode());
	}

	private InternForsendelse mapPostadresseAndForsendelse(HentForsendelseResponse hentForsendelseResponse) {

		HentForsendelseResponse.Postadresse regoppslagPostadresse = getAdresseFromRegoppslag(hentForsendelseResponse);

		if (regoppslagPostadresse == null) {
			administrerForsendelse.feilregistrerForsendelse(mapFeilregistrerForsendelse(hentForsendelseResponse));
			return forsendelseMapper.mapForsendelse(hentForsendelseResponse);
		}

		oppdaterPostadresse(hentForsendelseResponse.getForsendelseId(), regoppslagPostadresse);


		InternForsendelse forsendelseWithAdressFraRegoppslag = forsendelseMapper.mapForsendelse(hentForsendelseResponse);

		/**
		 *  mapper postadresse som har fått fra regoppslag.
		 */
		Postadresse nyPostadresse = Postadresse.builder()
				.adresselinje1(regoppslagPostadresse.getAdresselinje1())
				.adresselinje2(regoppslagPostadresse.getAdresselinje2())
				.adresselinje3(regoppslagPostadresse.getAdresselinje3())
				.landkode(mapLandkode(regoppslagPostadresse.getLandkode()))
				.postnummer(regoppslagPostadresse.getPostnummer())
				.poststed(regoppslagPostadresse.getPoststed())
				.build();

		forsendelseWithAdressFraRegoppslag.setPostadresse(nyPostadresse);

		return forsendelseWithAdressFraRegoppslag;
	}

	private void oppdaterPostadresse(Long forsendelseId, HentForsendelseResponse.Postadresse postadresse) {
		if (postadresse != null) {
			administrerForsendelse.oppdaterPostadresse(mapOppdaterPostadresse(forsendelseId, postadresse));
		}
	}

	private HentForsendelseResponse.Postadresse getAdresseFromRegoppslag(HentForsendelseResponse hentForsendelseResponse) {
		AdresseTo adresseTo = regoppslagRestConsumer.treg002HentAdresse(HentAdresseRequestTo.builder()
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
