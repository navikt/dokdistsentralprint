package no.nav.dokdistsentralprint.exception.technical;

public abstract class AbstractDokdistsentralprintTechnicalException extends RuntimeException {

	public AbstractDokdistsentralprintTechnicalException(String message) {
		super(message);
	}

	public AbstractDokdistsentralprintTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
