package no.nav.dokdistsentralprint.consumer.tkat020;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */
@Value
@Builder
public class DokumenttypeInfoTo {
	private final String dokumentTittel;
}
