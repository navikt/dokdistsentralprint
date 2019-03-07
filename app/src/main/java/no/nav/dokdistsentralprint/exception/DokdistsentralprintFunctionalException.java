package no.nav.dokdistsentralprint.exception;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DokdistsentralprintFunctionalException extends RuntimeException {

	public DokdistsentralprintFunctionalException(String message) {
		super(message);
	}

	public DokdistsentralprintFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
