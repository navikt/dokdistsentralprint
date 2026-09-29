package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OppdaterForsendelseRequest(
		Long forsendelseId,
		String forsendelseStatus,
		LocalDateTime ekspedertDato,
		String kilde
) {
}
