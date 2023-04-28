package no.nav.dokdistsentralprint.consumer.rdist001;

public record OppdaterForsendelseRequest (
	Long forsendelseId,
	String forsendelseStatus
) {

}
