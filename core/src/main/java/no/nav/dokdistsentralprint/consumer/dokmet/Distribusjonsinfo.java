package no.nav.dokdistsentralprint.consumer.dokmet;

public record Distribusjonsinfo(
		String portoklasse,
		String konvoluttvinduType,
		String sentralPrintDokumentType,
		boolean tosidigprint
) {
	public Distribusjonsinfo(
			String portoklasse,
			String konvoluttvinduType,
			String sentralPrintDokumentType,
			Boolean tosidigprint
	) {
		this(portoklasse, konvoluttvinduType, sentralPrintDokumentType, tosidigprint == null || tosidigprint);
	}
}