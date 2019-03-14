package no.nav.dokdistsentralprint.storage;

import lombok.Builder;
import lombok.Data;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@Builder
public class DokdistDokument {
	private byte[] pdf;
	private String dokumentObjektReferanse;

}
