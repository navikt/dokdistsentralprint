package no.nav.dokdistsentralprint.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationException extends AbstractDokdistsentralprintFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
