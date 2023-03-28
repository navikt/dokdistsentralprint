package no.nav.dokdistsentralprint.constants;

import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.constants.NavHeaders.NAV_CALLID;

public class NavHeadersFilter implements ExchangeFilterFunction {

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

		return next.exchange(ClientRequest.from(request).headers(headers -> headers.set(NAV_CALLID, MDC.get(CALL_ID))).build());
	}
}
