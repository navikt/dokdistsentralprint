package no.nav.dokdistsentralprint.config.azure;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureTokenProperties(
		@NotEmpty String openidConfigTokenEndpoint,
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret
) {
	public static final String SPRING_DEFAULT_PRINCIPAL = "anonymousUser";
	public static final String CLIENT_REGISTRATION_DOKDISTADMIN = "azure-dokdistadmin";
	public static final String CLIENT_REGISTRATION_REGOPPSLAG = "azure-regoppslag";

	public static OAuth2AuthorizeRequest getOAuth2AuthorizeRequestForAzure(String clientRegistrationId) {
		return OAuth2AuthorizeRequest
				.withClientRegistrationId(clientRegistrationId)
				.principal(SPRING_DEFAULT_PRINCIPAL)
				.build();
	}
}
