package no.nav.dokdistsentralprint.exception.technical;

public abstract class AbstractDokdistsentralprintTechnicalException extends RuntimeException {

	AbstractDokdistsentralprintTechnicalException(String message) {
		super(message);
	}

	AbstractDokdistsentralprintTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
