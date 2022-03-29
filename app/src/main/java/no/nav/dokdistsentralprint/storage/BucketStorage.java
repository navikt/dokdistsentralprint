package no.nav.dokdistsentralprint.storage;

public interface BucketStorage {
	/**
	 * Laster ned kryptert payload fra ekstern bucket i Google Cloud Storage
	 *
	 * @param objectName     Navn på objektet som finnes i bucket. GUID eller annen unik ID som er kjent.
	 * @param associatedData Data som knyttes til objektet for å unngå manipulering. Må ha lik verdi som da det ble kryptert.
	 *                       F.eks journalpostId, bestillingsId.
	 */
	String downloadObject(String objectName, String associatedData);
}
