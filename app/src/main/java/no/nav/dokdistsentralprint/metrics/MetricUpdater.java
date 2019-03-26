package no.nav.dokdistsentralprint.metrics;

import static no.nav.dokdistsentralprint.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.SERVICE_ID;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricUpdater {

	private static final String QDIST009_SERVICE = "service";
	private static final String LABEL_LANDKODE = "landkode";
	private static final String LABEL_POSTDESTINASJON = "postdestinasjon";

	private final MeterRegistry meterRegistry;

	public MetricUpdater(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void updateQdist009Metrics(String landkode,
											 String postdestinasjon) {
		meterRegistry.counter(QDIST009_SERVICE,
				LABEL_PROCESS, SERVICE_ID,
				LABEL_LANDKODE, landkode,
				LABEL_POSTDESTINASJON, postdestinasjon).increment();
	}

}
