package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.qdist009.domain.InternForsendelse;
import no.nav.opprettoppgave.tjenestespesifikasjon.OpprettOppgave;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

@Component
public class OpprettOppgaverMapper {

	private static final String BEHANDLE_MANGLENDE_ADRESSE = "BEHANDLE_MANGLENDE_ADRESSE";

	/**
	 * Forsendelser som mangler postadresse feilregistert og sendes meldingen til qopp001 kø
	 * for å opprette oppgave for videre saksbehandling.
	 **/
	@Handler
	public OpprettOppgave opprettOppgave(InternForsendelse internForsendelse) {
		InternForsendelse.ArkivInformasjon arkivInformasjon = internForsendelse.getArkivInformasjon();

		OpprettOppgave opprettOppgave = new OpprettOppgave();
		opprettOppgave.setOppgaveType(BEHANDLE_MANGLENDE_ADRESSE);
		opprettOppgave.setArkivSystem(arkivInformasjon.getArkivSystem().name());
		opprettOppgave.setArkivKode(arkivInformasjon.getArkivId());
		return opprettOppgave;
	}

}
