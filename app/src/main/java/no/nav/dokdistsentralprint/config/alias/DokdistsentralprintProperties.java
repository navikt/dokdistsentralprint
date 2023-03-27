package no.nav.dokdistsentralprint.config.alias;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties("dokdistsentralprint")
public class DokdistsentralprintProperties {

	private final Endpoints endpoints = new Endpoints();

	@Data
	public static class Endpoints {
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
