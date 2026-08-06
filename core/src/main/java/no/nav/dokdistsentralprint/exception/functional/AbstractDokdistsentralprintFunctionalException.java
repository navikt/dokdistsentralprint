package no.nav.dokdistsentralprint.exception.functional;

public abstract class AbstractDokdistsentralprintFunctionalException extends RuntimeException {

	AbstractDokdistsentralprintFunctionalException(String message) {
		super(message);
	}

	AbstractDokdistsentralprintFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

}
