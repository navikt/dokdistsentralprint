package no.nav.dokdistsentralprint.exception.technical;

public class S3FailedToPutDocumentTechnicalException extends AbstractDokdistsentralprintTechnicalException {
	public S3FailedToPutDocumentTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public S3FailedToPutDocumentTechnicalException(String message) {
		super(message);
	}
}
