package no.nav.dokdistsentralprint.exception.technical;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class AbstractDokdistsentralprintTechnicalException extends RuntimeException {

	public AbstractDokdistsentralprintTechnicalException(String message) {
		super(message);
	}

	public AbstractDokdistsentralprintTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
