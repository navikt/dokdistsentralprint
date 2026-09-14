package no.nav.dokdistsentralprint.config.alias;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nais")
public record NaisProperties(@NotBlank String tokenEndpoint) {
}
