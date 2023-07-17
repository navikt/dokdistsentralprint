package no.nav.dokdistsentralprint.config.alias;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties("dokdistmellomlager")
@Validated
public class DokdistmellomlagerProperties {
	@NotEmpty
	private String projectid;
	@NotEmpty
	private String bucket;
	@NotEmpty
	private String keyring;
	@NotEmpty
	private String keyid;

	public String gcpKekUri() {
		return "gcp-kms://projects/" + projectid + "/locations/europe-north1/keyRings/" + keyring + "/cryptoKeys/" + keyid;
	}
}
