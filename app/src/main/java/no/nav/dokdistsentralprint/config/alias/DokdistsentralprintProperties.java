package no.nav.dokdistsentralprint.config.alias;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("dokdistsentralprint")
public class DokdistsentralprintProperties {

	private final Endpoints endpoints = new Endpoints();

	@Data
	public static class Endpoints {
		@NotNull
		private AzureEndpoint dokmet;

		@NotNull
		private AzureEndpoint dokdistadmin;
	}

	@Data
	public static class AzureEndpoint {
		@NotEmpty
		private String url;

		@NotEmpty
		private String scope;
	}
}
