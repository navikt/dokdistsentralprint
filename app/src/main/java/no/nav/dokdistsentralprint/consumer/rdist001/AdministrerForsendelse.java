package no.nav.dokdistsentralprint.consumer.rdist001;

public interface AdministrerForsendelse {

	HentForsendelseResponse hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(OppdaterForsendelseRequest oppdaterForsendelseRequest);

	HentPostDestinasjonResponseTo hentPostDestinasjon(final String landkode);

	void oppdaterPostadresse(OppdaterPostadresseRequest postadresse);
}
