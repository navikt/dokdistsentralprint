package no.nav.dokdistsentralprint.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class InvalidForsendelseStatusException extends AbstractDokdistsentralprintFunctionalException {

	public InvalidForsendelseStatusException(String message) {
		super(message);
	}

	public InvalidForsendelseStatusException(String message, Throwable cause) {
		super(message, cause);
	}
}
