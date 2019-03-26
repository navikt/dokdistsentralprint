package no.nav.dokdistsentralprint;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistsentralprint.metrics.DokMonitoringAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
	@Bean
	DokMonitoringAspect timedAspect(MeterRegistry meterRegistry) {
		return new DokMonitoringAspect(meterRegistry);
	}
}
