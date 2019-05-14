package no.nav.dokdistsentralprint.storage;

import static no.nav.dokdistsentralprint.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistsentralprint.storage.S3Configuration.BUCKET_NAME;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistsentralprint.exception.functional.DocumentNotFoundInS3FunctionalException;
import no.nav.dokdistsentralprint.exception.technical.AbstractDokdistsentralprintTechnicalException;
import no.nav.dokdistsentralprint.exception.technical.S3FailedToGetDocumentTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import javax.inject.Inject;


public class S3Storage implements Storage {

	private AmazonS3 s3WithStrictEncryption;

	@Inject
	public S3Storage(AmazonS3 s3Encryption) {
		this.s3WithStrictEncryption = s3Encryption;
	}

	@Override
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public void put(String key, String value) {
		throw new UnsupportedOperationException("dokdistsentralprint støtter ikke persistering av objekter til dokdistmellomlager");
	}

	@Override
	@Retryable(include = AbstractDokdistsentralprintTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	public String get(String key) {
		try {
			String result = s3WithStrictEncryption.getObjectAsString(BUCKET_NAME, key);

			if (result != null) {
				return result;
			} else {
				throw new DocumentNotFoundInS3FunctionalException(String.format("Henting fra AmazonS3 på key=%s returnerte tom verdi", key));
			}
		} catch (SdkClientException e) {
			throw new S3FailedToGetDocumentTechnicalException(String.format("Teknisk feil mot AmazonS3 ved henting på key=%s", key), e);
		} catch (SecurityException e) {
			throw new S3FailedToGetDocumentTechnicalException(String.format("Objektet som ble forsøkt hentet fra AmazonS3 på key=%s var ikke kryptert.", key), e);
		}
	}

	@Override
	public void delete(String key) {
		throw new UnsupportedOperationException("dokdistfordeling støtter ikke sletting av objekter fra dokdistmellomlager");
	}
}

