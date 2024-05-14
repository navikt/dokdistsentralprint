package no.nav.dokdistsentralprint.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Configuration
@EnableCaching
public class LokalCacheConfig {

	public static final String TKAT020_CACHE = "tkat020Cache";
	public static final String POSTDESTINASJON_CACHE = "postdestinasjonCache";

	@Bean
	@Primary
	@Profile({"nais", "local"})
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(1, DAYS)
						.build()),
				new CaffeineCache(POSTDESTINASJON_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(60, MINUTES)
						.build())
		));
		return manager;
	}
}
