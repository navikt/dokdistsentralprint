package no.nav.dokdistsentralprint.consumer.rdist001;

public interface AdministrerForsendelse {

	HentForsendelseResponse hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(OppdaterForsendelseRequest oppdaterForsendelseRequest);

	String hentPostdestinasjon(final String landkode);

	void oppdaterPostadresse(OppdaterPostadresseRequest postadresse);
}
