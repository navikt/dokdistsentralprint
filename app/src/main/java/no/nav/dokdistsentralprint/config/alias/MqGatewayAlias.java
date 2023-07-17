package no.nav.dokdistsentralprint.config.alias;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("mqgateway01")
@Validated
public class MqGatewayAlias {
	@NotEmpty
	private String hostname;
	@NotEmpty
	private String name;
	@Min(0)
	private int port;
	private MqChannel channel = new MqChannel();

	@Data
	@Validated
	public static class MqChannel {
		@NotEmpty
		private String name;
		@NotBlank
		private String securename;
		private boolean enabletls;
	}
}
