package no.nav.dokdistsentralprint.consumer.dokmet;

public record Distribusjonsinfo(
		String portoklasse,
		String konvoluttvinduType,
		String sentralPrintDokumentType,
		boolean tosidigprint
) {
}