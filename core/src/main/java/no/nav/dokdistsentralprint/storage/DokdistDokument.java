package no.nav.dokdistsentralprint.storage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokdistDokument {

	private byte[] pdf;
	private String dokumentObjektReferanse;
}
