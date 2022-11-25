package no.nav.dokdistsentralprint.exception.functional;

public class InvalidForsendelseStatusException extends AbstractDokdistsentralprintFunctionalException {

	public InvalidForsendelseStatusException(String message) {
		super(message);
	}

	public InvalidForsendelseStatusException(String message, Throwable cause) {
		super(message, cause);
	}
}
