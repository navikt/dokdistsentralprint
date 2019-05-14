package no.nav.dokdistsentralprint.exception.functional;

public class S3FailedToGetDocumentFunctionalException extends AbstractDokdistsentralprintFunctionalException {

	public S3FailedToGetDocumentFunctionalException(String message) {
		super(message);
	}
	public S3FailedToGetDocumentFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
