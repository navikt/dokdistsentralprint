package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistsentralprint.consumer.rdist001.OppdaterForsendelseRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static java.lang.Long.valueOf;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistsentralprint.qdist009.domain.Forsendelsestatus.OVERSENDT;

@Component
public class DokdistStatusUpdater {

	private final AdministrerForsendelseConsumer administrerForsendelseConsumer;

	public DokdistStatusUpdater(AdministrerForsendelseConsumer administrerForsendelseConsumer) {
		this.administrerForsendelseConsumer = administrerForsendelseConsumer;
	}

	@Handler
	public void doUpdate(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);

		administrerForsendelseConsumer.oppdaterForsendelseStatus(
				new OppdaterForsendelseRequest(valueOf(forsendelseId), OVERSENDT.name())
		);
	}

}
