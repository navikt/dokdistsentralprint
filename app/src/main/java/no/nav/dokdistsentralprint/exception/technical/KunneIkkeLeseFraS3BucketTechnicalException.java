package no.nav.dokdistsentralprint.exception.technical;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class KunneIkkeLeseFraS3BucketTechnicalException extends AbstractDokdistsentralprintTechnicalException {

	public KunneIkkeLeseFraS3BucketTechnicalException(String message) {
		super(message);
	}

	public KunneIkkeLeseFraS3BucketTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
