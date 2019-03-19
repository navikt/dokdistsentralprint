package no.nav.dokdistsentralprint.itest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Profile("itest")
@Configuration
public class FtpsConfig {
//
//	@Bean
//	FTPSClient startFtpServer() throws FtpException {
//		FtpServerFactory serverFactory = new FtpServerFactory();
//		ListenerFactory factory = new ListenerFactory();
//		factory.setPort(2221);
//		factory.setServerAddress("test");
//		serverFactory.addListener("default", factory.createListener());
//		FtpServer server = serverFactory.createServer();
//		server.start();
//	}

}
