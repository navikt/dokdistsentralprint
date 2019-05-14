package no.nav.dokdistsentralprint.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DocumentNotFoundInS3FunctionalException extends AbstractDokdistsentralprintFunctionalException {

	public DocumentNotFoundInS3FunctionalException(String message) {
		super(message);
	}

	public DocumentNotFoundInS3FunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
