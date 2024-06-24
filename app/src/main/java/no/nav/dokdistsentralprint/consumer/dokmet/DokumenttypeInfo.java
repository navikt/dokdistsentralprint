package no.nav.dokdistsentralprint.consumer.dokmet;

public record DokumenttypeInfo(
		String portoklasse,
		String konvoluttvinduType,
		String sentralPrintDokumentType,
		boolean tosidigprint
) {
}