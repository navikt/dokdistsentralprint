package no.nav.dokdistsentralprint.exception;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationException extends DokdistsentralprintFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
