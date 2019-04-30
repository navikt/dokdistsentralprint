package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.qdist009.domain.DistribuerForsendelseTilSentralPrintTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class DistribuerForsendelseTilSentralPrintMapper {

	@Handler
	public DistribuerForsendelseTilSentralPrintTo map(DistribuerTilKanal distribuerTilKanal) {
		return DistribuerForsendelseTilSentralPrintTo.builder()
				.forsendelseId(distribuerTilKanal.getForsendelseId())
				.build();
	}

}
