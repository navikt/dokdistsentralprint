package no.nav.dokdistsentralprint.consumer.tkat020;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DokumenttypeInfoTo {

	String portoklasse;
	String konvoluttvinduType;
	String sentralPrintDokumentType;
	boolean tosidigprint;
}
