package no.nav.dokdistsentralprint.exception;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DokdistsentralprintTechnicalException extends RuntimeException {

	public DokdistsentralprintTechnicalException(String message) {
		super(message);
	}

	public DokdistsentralprintTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
