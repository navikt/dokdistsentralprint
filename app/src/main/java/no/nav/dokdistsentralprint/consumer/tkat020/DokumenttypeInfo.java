package no.nav.dokdistsentralprint.consumer.tkat020;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DokumenttypeInfo {

	String portoklasse;
	String konvoluttvinduType;
	String sentralPrintDokumentType;
	boolean tosidigprint;
}
