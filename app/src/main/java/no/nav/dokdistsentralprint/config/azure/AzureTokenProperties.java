package no.nav.dokdistsentralprint.config.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureTokenProperties(
		@NotEmpty String openidConfigTokenEndpoint,
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret
) {

	public static final String CLIENT_REGISTRATION_DOKDISTADMIN = "azure-dokdistadmin";
	public static final String CLIENT_REGISTRATION_REGOPPSLAG = "azure-regoppslag";
}