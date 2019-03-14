package no.nav.dokdistsentralprint.qdist009;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class BestillingEntity {
	private final String fileName;
	private final byte[] entity;
}
