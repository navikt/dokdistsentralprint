package no.nav.dokdistsentralprint.consumer.rdist001;

public record OppdaterFilinformasjonRequest(
		Long filInfoId,
		String filnavn,
		String filtype,
		String status,
		String kilde
) {
}