package no.nav.dokdistsentralprint.config.alias;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("serviceuser")
@Validated
public class ServiceuserAlias {
	@NotEmpty
	private String username;
	@NotEmpty
	private String password;
}
