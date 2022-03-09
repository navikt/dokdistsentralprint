package no.nav.dokdistsentralprint.consumer.reststs;

import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.exception.technical.StsRetriveTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

import static java.util.Objects.requireNonNull;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.REST_STS_CACHE;

@Component
public class StsRestConsumer {

	private final RestTemplate restTemplate;
	private final String stsUrl;
	public static final int DELAY_SHORT = 300;
	public static final int MULTIPLIER_SHORT = 2;

	@Inject
	public StsRestConsumer(@Value("${security-token-service-token.url}") String stsUrl,
						   RestTemplateBuilder restTemplateBuilder,
						   final ServiceuserAlias serviceuserAlias) {
		this.stsUrl = stsUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Retryable(include = StsRetriveTokenException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Cacheable(REST_STS_CACHE)
	public String getBearerToken() {
		try {
			return requireNonNull(restTemplate.getForObject(stsUrl + "?grant_type=client_credentials&scope=openid", StsResponse.class))
					.getAccessToken();
		} catch (HttpStatusCodeException e) {
			throw new StsRetriveTokenException(String.format("Kall mot STS feilet med status=%s feilmelding=%s.", e.getStatusCode(), e
					.getMessage()), e);
		}
	}
}

