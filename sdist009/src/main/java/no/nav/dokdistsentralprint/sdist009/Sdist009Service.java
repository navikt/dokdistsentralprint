package no.nav.dokdistsentralprint.sdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterFilinformasjonRequest;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterForsendelseRequest;
import no.nav.dokdistsentralprint.kvittering.LePuKode;
import no.nav.dokdistsentralprint.kvittering.Rapport;
import no.nav.dokdistsentralprint.kvittering.StatusKode;
import no.nav.dokdistsentralprint.kvittering.StatusRapport;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

import static java.lang.String.valueOf;
import static no.nav.dokdistsentralprint.kvittering.LePuKode.KONVOLUTTERT;
import static no.nav.dokdistsentralprint.kvittering.LePuKode.MAILPIECE_MOTTAK;
import static no.nav.dokdistsentralprint.kvittering.LePuKode.RETURPOST;
import static no.nav.dokdistsentralprint.kvittering.StatusKode.OK;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.OVERSENDT;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.RETURPOSTBEHANDLET;
import static no.nav.dokdistsentralprint.sdist009.Sdist009Route.MAILPIECE_FILE_NAME;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class Sdist009Service {

	private static final String SDIST009_KILDE = "SDIST009";
	private static final String FIL_STATUS_OPPRETTET = "OPPRETTET";
	private static final String FIL_STATUS_OK = "OK";
	private static final String FIL_TYPE = "DOK_RAPP_PRINT";
	private static final EnumSet<ForsendelseStatus> FORVENTET_MAILPIECE_MOTTAK_STATUS = EnumSet.of(OVERSENDT, BEKREFTET);
	private static final EnumSet<ForsendelseStatus> FORVENTET_KONVOLUTTERT_STATUS = EnumSet.of(BEKREFTET, EKSPEDERT);
	private static final EnumSet<ForsendelseStatus> FORVENTET_RETURPOST_STATUS = EnumSet.of(EKSPEDERT, RETURPOSTBEHANDLET);


	private final AdministrerForsendelseConsumer administrerForsendelseConsumer;
	private final ReturpostOppgaveService returpostOppgaveService;

	public Sdist009Service(AdministrerForsendelseConsumer administrerForsendelseConsumer,
						   ReturpostOppgaveService returpostOppgaveService) {
		this.administrerForsendelseConsumer = administrerForsendelseConsumer;
		this.returpostOppgaveService = returpostOppgaveService;
	}

	@Handler
	public void behandleKvitteringsfil(StatusRapport statusRapport, Exchange exchange) {
		String filnavn = exchange.getProperty(MAILPIECE_FILE_NAME, String.class);

		//TODO:handtering av tomme filer?
		Long filInfoId = opprettFilInfo(filnavn);

		if (!isEmpty(statusRapport.getRapport())) {
			statusRapport.getRapport()
					.forEach(this::behandleKvittering);

			oppdaterFilInfo(filInfoId);
		}
	}

	private void behandleKvittering(Rapport rapport) {
		StatusKode statusKode = rapport.getStatus().getStatus();
		HentForsendelseResponse forsendelse = hentForsendelse(rapport.getId());

		if (OK.equals(statusKode)) {
			switch (rapport.getStatus().getLePu()) {
				case MAILPIECE_MOTTAK -> behandleMailpieceMottak(forsendelse);
				case KONVOLUTTERT -> behandleKonvoluttert(forsendelse);
				case RETURPOST -> behandleReturpost(forsendelse);
			}
		} else {
			log.warn("Kvittering med bestillingsId={} og statusKode={} har feilet. Avslutter behandling av kvittering og går til neste.",
					forsendelse.getForsendelseId(), statusKode);
		}
	}

	private HentForsendelseResponse hentForsendelse(String bestillingId) {
		Long forsendelseId = administrerForsendelseConsumer.finnForsendelse(bestillingId);
		return administrerForsendelseConsumer.hentForsendelse(valueOf(forsendelseId));
	}

	private void behandleMailpieceMottak(HentForsendelseResponse forsendelse) {
		ForsendelseStatus forsendelseStatus = mapForsendelseStatus(forsendelse.getForsendelseStatus());
		switch (forsendelseStatus) {
			case OVERSENDT -> oppdaterForsendelseStatus(forsendelse.getForsendelseId(), BEKREFTET);
			case BEKREFTET -> loggKvitteringAlleredeBehandlet(forsendelse, MAILPIECE_MOTTAK, forsendelseStatus);
			default ->
					loggForsendelseHarUventetStatus(forsendelse, MAILPIECE_MOTTAK, FORVENTET_MAILPIECE_MOTTAK_STATUS);
		}
	}

	private void behandleKonvoluttert(HentForsendelseResponse forsendelse) {
		ForsendelseStatus forsendelseStatus = mapForsendelseStatus(forsendelse.getForsendelseStatus());
		switch (forsendelseStatus) {
			case BEKREFTET -> oppdaterForsendelseStatus(forsendelse.getForsendelseId(), EKSPEDERT);
			case EKSPEDERT -> loggKvitteringAlleredeBehandlet(forsendelse, KONVOLUTTERT, forsendelseStatus);
			default -> loggForsendelseHarUventetStatus(forsendelse, KONVOLUTTERT, FORVENTET_KONVOLUTTERT_STATUS);
		}
	}

	private void behandleReturpost(HentForsendelseResponse forsendelse) {
		ForsendelseStatus forsendelseStatus = mapForsendelseStatus(forsendelse.getForsendelseStatus());
		switch (forsendelseStatus) {
			case EKSPEDERT -> {
				returpostOppgaveService.opprettReturpostOppgave(forsendelse.getArkivInformasjon(), forsendelse.getBestillingsId());
				oppdaterForsendelseStatus(forsendelse.getForsendelseId(), RETURPOSTBEHANDLET);
			}
			case RETURPOSTBEHANDLET -> loggKvitteringAlleredeBehandlet(forsendelse, RETURPOST, forsendelseStatus);
			default -> loggForsendelseHarUventetStatus(forsendelse, RETURPOST, FORVENTET_RETURPOST_STATUS);
		}
	}

	private Long opprettFilInfo(String filnavn) {
		return administrerForsendelseConsumer.oppdaterFilinformasjon(OppdaterFilinformasjonRequest.builder()
				.filnavn(filnavn)
				.filtype(FIL_TYPE)
				.status(FIL_STATUS_OPPRETTET)
				.kilde(SDIST009_KILDE)
				.build());
	}

	private void oppdaterForsendelseStatus(Long forsendelseId, ForsendelseStatus forsendelseStatus) {
		administrerForsendelseConsumer.oppdaterForsendelseStatus(
				new OppdaterForsendelseRequest(
						forsendelseId,
						forsendelseStatus.name()
				)
		);
	}

	private void oppdaterFilInfo(Long filInfoId) {
		administrerForsendelseConsumer.oppdaterFilinformasjon(
				OppdaterFilinformasjonRequest.builder()
						.filInfoId(filInfoId)
						.status(FIL_STATUS_OK)
						.kilde(SDIST009_KILDE)
						.build()
		);
	}

	private void loggKvitteringAlleredeBehandlet(HentForsendelseResponse forsendelse, LePuKode lePuKode, ForsendelseStatus forsendelseStatus) {
		log.info("Forsendelse med forsendelseId={}, LePu={} og dokumentStatus={} er allerede {}. Avslutter behandling av kvittering og går til neste.",
				forsendelse.getForsendelseId(), lePuKode, forsendelse.getForsendelseStatus(), forsendelseStatus.name().toLowerCase());
	}

	private void loggForsendelseHarUventetStatus(HentForsendelseResponse forsendelse, LePuKode lePuKode, EnumSet<ForsendelseStatus> forventetStatus) {
		log.error("Forsendelse med forsendelseId={} og LePu={} har dokumentStatus={}. Forventet dokumentStatus={}. Avslutter behandling av kvittering og går til neste.",
				forsendelse.getForsendelseId(), lePuKode, forsendelse.getForsendelseStatus(), forventetStatus);
	}

	private ForsendelseStatus mapForsendelseStatus(String forsendelseStatus) {
		return ForsendelseStatus.valueOf(forsendelseStatus);
	}
}
