package no.nav.dokdistsentralprint.sdist009.itest.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.CoreConfig;
import no.nav.dokdistsentralprint.config.alias.DokdistsentralprintProperties;
import no.nav.dokdistsentralprint.config.azure.AzureTokenProperties;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.Path.of;
import static java.util.Collections.singletonList;

@Slf4j
@EnableConfigurationProperties({
		DokdistsentralprintProperties.class,
		AzureTokenProperties.class
})
@Import({
		Sdist009TestConfig.SshdSftpServerConfig.class,
		Sdist009TestConfig.CamelTestStartupConfig.class,
		JmsItestConfig.class,
		CoreConfig.class
})
@EnableAutoConfiguration
@Profile("itest")
public class Sdist009TestConfig {

	@Configuration
	static class CamelTestStartupConfig {

		private final AtomicInteger sshServerStartupCounter = new AtomicInteger(0);

		@Bean
		CamelContextConfiguration contextConfiguration(SshServer sshServer) {
			return new CamelContextConfiguration() {

				@Override
				public void beforeApplicationStart(CamelContext camelContext) {
					while (!sshServer.isStarted() && sshServerStartupCounter.get() <= 5) {
						try {
							// Busy wait
							Thread.sleep(1000);
							log.info("Forsøkt å starte sshserver. retry={}", sshServerStartupCounter.getAndIncrement());
						} catch (InterruptedException _) {
							// noop
						}
					}
					camelContext.getPropertiesComponent().addOverrideProperty("sftp.port", String.valueOf(sshServer.getPort()));
				}

				@Override
				public void afterApplicationStart(CamelContext camelContext) {

				}
			};
		}
	}

	@Configuration
	static class SshdSftpServerConfig {
		@Bean
		public Path sshdPath() throws IOException {
			return Files.createTempDirectory("sshd");
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		public SshServer sshServer(Path sshdPath) {
			SshServer sshd = SshServer.setUpDefaultServer();
			sshd.setPort(0);
			sshd.setCommandFactory(new ScpCommandFactory());
			sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(of("src/test/resources/ssh/hostkey.ser")));

			sshd.setSubsystemFactories(singletonList(new SftpSubsystemFactory()));
			sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
			sshd.setUserAuthFactories(singletonList(new UserAuthPublicKeyFactory()));
			sshd.setFileSystemFactory(new VirtualFileSystemFactory(sshdPath));

			return sshd;
		}
	}
}
