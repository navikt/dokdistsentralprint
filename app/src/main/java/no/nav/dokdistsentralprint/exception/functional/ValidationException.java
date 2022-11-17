package no.nav.dokdistsentralprint.exception.functional;

public class ValidationException extends AbstractDokdistsentralprintFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
