package no.nav.dokdistsentralprint.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.dokdistsentralprint.exception.functional.AbstractDokdistsentralprintFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;
import org.springframework.stereotype.Component;

import static no.nav.dokdistsentralprint.metrics.MetricLabels.LABEL_ERROR_TYPE;
import static no.nav.dokdistsentralprint.metrics.MetricLabels.LABEL_EXCEPTION_NAME;
import static no.nav.dokdistsentralprint.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistsentralprint.metrics.MetricLabels.TYPE_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistsentralprint.metrics.MetricLabels.TYPE_TECHNICAL_EXCEPTION;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.SERVICE_ID;

@Component
public class Qdist009MetricsRoutePolicy extends RoutePolicySupport {

	private final MeterRegistry registry;
	private Timer.Sample timer;

	private static final String QDIST009_PROCESS_TIMER = "dok_request_latency";
	private static final String QDIST009_PROCESS_TIMER_DESCRIPTION = "prosesseringstid for kall inn til qdist009";
	private static final String QDIST009_EXCEPTION = "request_exception_total";

	public Qdist009MetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start(registry);
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);

		timer.stop(Timer.builder(QDIST009_PROCESS_TIMER)
				.description(QDIST009_PROCESS_TIMER_DESCRIPTION)
				.tags(LABEL_PROCESS, SERVICE_ID)
				.publishPercentileHistogram(true)
				.register(registry));

		if (exception != null) {
			if (isFunctionalException(exception)) {
				registry.counter(QDIST009_EXCEPTION,
						LABEL_ERROR_TYPE, TYPE_FUNCTIONAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getSimpleName(),
						LABEL_PROCESS, SERVICE_ID).increment();
			} else {
				registry.counter(QDIST009_EXCEPTION,
						LABEL_ERROR_TYPE, TYPE_TECHNICAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getCanonicalName(),
						LABEL_PROCESS, SERVICE_ID).increment();
			}
		}
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof AbstractDokdistsentralprintFunctionalException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() instanceof Exception) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
