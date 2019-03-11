package no.nav.dokdistsentralprint.storage;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class DokdistDokument {

	private byte[] pdf;

}
