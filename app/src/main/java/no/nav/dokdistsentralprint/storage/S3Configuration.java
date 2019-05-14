package no.nav.dokdistsentralprint.storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import no.nav.dokdistsentralprint.exception.functional.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;


@Configuration
@Profile("nais")
public class S3Configuration {

	private static final String REGION_TO_USE_FOR_S3_TO_WORK_ONPREM = Regions.US_EAST_1.getName();
	public static final String BUCKET_NAME = "dokdistmellomlager";

	private SecretKey secretKey;

	@Value("${dokdistsentralprint_s3_creds_username}")
	private String credsUsername;

	@Value("${dokdistsentralprint_s3_creds_password}")
	private String credsPass;

	@Value("${storage_s3_url}")
	private String s3Endpoint;

	@Value("${dokdistmellomlager_s3_storage_crypto_password}")
	private String encryptionPassphrase;

	@Bean
	public Storage awsStorage() {
		secretKey = key(encryptionPassphrase);
		AmazonS3 s3 = s3(secretKey);

		return new S3Storage(s3);
	}

	private AmazonS3 s3(SecretKey secretKey) {
		AWSCredentials credentials = new BasicAWSCredentials(credsUsername, credsPass);

		return AmazonS3EncryptionClientBuilder
				.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, REGION_TO_USE_FOR_S3_TO_WORK_ONPREM))
				.enablePathStyleAccess()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withCryptoConfiguration(new CryptoConfiguration(CryptoMode.StrictAuthenticatedEncryption))
				.withEncryptionMaterials(new StaticEncryptionMaterialsProvider(new EncryptionMaterials(secretKey)))
				.build();
	}

	private SecretKey key(String passphrase) {
		if (passphrase.getBytes().length == 32) {
			return new SecretKeySpec(passphrase.getBytes(), "AES");
		} else {
			throw new ValidationException("Passordet for s3Storage sin AES må være 256 bit");
		}
	}
}
