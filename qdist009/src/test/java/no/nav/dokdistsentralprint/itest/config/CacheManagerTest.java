package no.nav.dokdistsentralprint.itest.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.POSTDESTINASJON_CACHE;

@Profile("itest")
@Configuration
public class CacheManagerTest {

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(DOKMET_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, SECONDS)
						.recordStats()
						.build()),
				new CaffeineCache(POSTDESTINASJON_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, SECONDS)
						.recordStats()
						.build())
		));
		return manager;
	}
}
