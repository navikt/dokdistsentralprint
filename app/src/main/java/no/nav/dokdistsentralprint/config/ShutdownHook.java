package no.nav.dokdistsentralprint.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.jms.ConnectionFactory;

/**
 * Rydder opp ressurser som Spring ikke gjør selv.
 */
@Slf4j
@Component
public class ShutdownHook {

	@Autowired
	private ConnectionFactory wmqConnectionFactory;

	@PreDestroy
	public void destroy() {
		log.info("Graceful shutdown - Lukker koblinger til ConnectionFactory pool");
		((PooledConnectionFactory) wmqConnectionFactory).clear();
	}
}
