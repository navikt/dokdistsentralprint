package no.nav.dokdistsentralprint.qdist009;

import static java.lang.String.format;

import no.nav.dokdistsentralprint.exception.functional.ValidationException;
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
	public DistribuerForsendelseTilSentralPrintTo Map(DistribuerTilKanal distribuerTilKanal) {
		return DistribuerForsendelseTilSentralPrintTo.builder()
				.forsendelseId(distribuerTilKanal.getForsendelseId())
				.build();
	}

}
