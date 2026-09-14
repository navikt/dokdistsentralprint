package no.nav.dokdistsentralprint.consumer.naistoken;

import no.nav.dokdistsentralprint.exception.technical.NaisTexasTechnicalException;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.consumer.naistoken.NaisTexasTokenConsumer.TARGET_PATTERN;

public class NaisTexasWebClientRequestInterceptor implements ExchangeFilterFunction {

	public static final String TARGET_SCOPE = "targetScope";

	private final NaisTexasTokenConsumer naisTexasTokenConsumer;

	public NaisTexasWebClientRequestInterceptor(NaisTexasTokenConsumer naisTexasTokenConsumer) {
		this.naisTexasTokenConsumer = naisTexasTokenConsumer;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return Mono.fromCallable(() -> addHeaders(request))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(next::exchange)
				.onErrorMap(error ->
						new NaisTexasTechnicalException(format("Kunne ikke legge token i headers. feilmelding=%s", error.getMessage()), error)
				);
	}

	private ClientRequest addHeaders(ClientRequest request) {
		ClientRequest.Builder requestBuilder = ClientRequest.from(request);
		request.attribute(TARGET_SCOPE)
				.map(String.class::cast)
				.filter(targetScope -> TARGET_PATTERN.matcher(targetScope).matches())
				.ifPresent(targetScope ->
						requestBuilder.headers(headers ->
								headers.setBearerAuth(naisTexasTokenConsumer.getSystemToken(targetScope)))
				);
		return requestBuilder
				.build();
	}
}
