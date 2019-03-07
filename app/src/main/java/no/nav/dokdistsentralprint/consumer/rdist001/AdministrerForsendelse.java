package no.nav.dokdistsentralprint.consumer.rdist001;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface AdministrerForsendelse {

	HentForsendelseResponseTo hentForsendelse(final String forsendelseId);

	void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus, String bestillingsId);
}
