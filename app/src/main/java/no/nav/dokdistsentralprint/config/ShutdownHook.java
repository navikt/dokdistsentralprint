package no.nav.dokdistsentralprint.config;

import jakarta.annotation.PreDestroy;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Rydder opp ressurser som Spring ikke gjør selv.
 */
@Slf4j
@Component
public class ShutdownHook {

	private final ConnectionFactory mqConnectionFactory;

	public ShutdownHook(ConnectionFactory mqConnectionFactory) {
		this.mqConnectionFactory = mqConnectionFactory;
	}

	@PreDestroy
	public void destroy() {
		log.info("Graceful shutdown - Lukker koblinger til ConnectionFactory pool");
		((JmsPoolConnectionFactory) mqConnectionFactory).clear();
	}
}
