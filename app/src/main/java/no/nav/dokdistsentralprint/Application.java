package no.nav.dokdistsentralprint;

import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import no.nav.dokdistsentralprint.config.props.SrvAppserverProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableRetry
@EnableConfigurationProperties({ServiceuserAlias.class,
		MqGatewayAlias.class,
		SrvAppserverProperties.class})
@Import(ApplicationConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
