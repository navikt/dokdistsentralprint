package no.nav.dokdistsentralprint.qdist009.domain;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */

@Value
@Builder
public class DistribuerForsendelseTilSentralPrintTo {

	private String forsendelseId;
}
