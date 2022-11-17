package no.nav.dokdistsentralprint.consumer.rdist001;

public interface AdministrerForsendelse {

	HentForsendelseResponseTo hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(final String forsendelseId, final String forsendelseStatus);

	HentPostDestinasjonResponseTo hentPostDestinasjon(final String landkode);
}
