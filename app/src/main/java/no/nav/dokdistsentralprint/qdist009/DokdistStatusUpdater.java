package no.nav.dokdistsentralprint.qdist009;

import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_FORSENDELSE_ID;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DokdistStatusUpdater {

	private static final String FORSENDELSE_OVERSENDT = "OVERSENDT";
	private final AdministrerForsendelse administrerForsendelse;

	public DokdistStatusUpdater(AdministrerForsendelse administrerForsendelse) {
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public void doUpdate(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);
		administrerForsendelse.oppdaterForsendelseStatus(forsendelseId, FORSENDELSE_OVERSENDT);
	}

}
