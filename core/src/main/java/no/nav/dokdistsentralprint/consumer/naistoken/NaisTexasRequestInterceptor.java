package no.nav.dokdistsentralprint.consumer.naistoken;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

public class NaisTexasRequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String TARGET_SCOPE = "targetScope";

	private final NaisTexasTokenConsumer naisTexasTokenConsumer;

	public NaisTexasRequestInterceptor(NaisTexasTokenConsumer naisTexasTokenConsumer) {
		this.naisTexasTokenConsumer = naisTexasTokenConsumer;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
										ClientHttpRequestExecution execution) throws IOException {
		Map<String, Object> attributes = request.getAttributes();
		if (attributes.containsKey(TARGET_SCOPE)) {
			String scopeAttribute = (String) attributes.get(TARGET_SCOPE);
			request.getHeaders().setBearerAuth(naisTexasTokenConsumer.getSystemToken(scopeAttribute));
		}
		return execution.execute(request, body);
	}
}
