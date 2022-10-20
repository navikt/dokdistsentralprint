package no.nav.dokdistsentralprint.config.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = "azure")
public record AzureTokenProperties(
		@NotEmpty String openidConfigTokenEndpoint,
		@NotEmpty String appClientId,
		@NotEmpty String appClientSecret
) {
	public static final String SPRING_DEFAULT_PRINCIPAL = "anonymousUser";
	public static final String CLIENT_REGISTRATION_ID = "azure";

	public static OAuth2AuthorizeRequest getOAuth2AuthorizeRequestForAzure() {
		return OAuth2AuthorizeRequest
				.withClientRegistrationId(CLIENT_REGISTRATION_ID)
				.principal(SPRING_DEFAULT_PRINCIPAL)
				.build();
	}
}
