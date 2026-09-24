package no.nav.dokdistsentralprint.consumer.rdist001;

import java.time.LocalDateTime;

public record OppdaterForsendelseRequest(
		Long forsendelseId,
		String forsendelseStatus,
		LocalDateTime ekspedertDato
) {
	public OppdaterForsendelseRequest(Long forsendelseId, String forsendelseStatus) {
		this(forsendelseId, forsendelseStatus, null);
	}
}
