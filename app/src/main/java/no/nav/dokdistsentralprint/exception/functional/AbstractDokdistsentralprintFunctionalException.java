package no.nav.dokdistsentralprint.exception.functional;

public abstract class AbstractDokdistsentralprintFunctionalException extends RuntimeException {

	public AbstractDokdistsentralprintFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistsentralprintFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

}
