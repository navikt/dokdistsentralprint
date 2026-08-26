package no.nav.dokdistsentralprint.sdist009.itest;

import no.nav.dokdistsentralprint.sdist009.itest.config.Sdist009TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("itest")
@SpringBootTest(classes = Sdist009TestConfig.class, webEnvironment = RANDOM_PORT)
class Sdist009ITest {

	private static final String INBOUND = "inbound/dokdistsentralprint";
	private static final String FEILET = "feilet";
	private static final String FERDIG = "ferdig";

	@Autowired
	private Path sshdPath;

	private Path inbound;

	@BeforeEach
	void setup() {
		inbound = sshdPath.resolve(INBOUND);
	}

	@Test
	void leseFilFraSftp() throws IOException {
		copyFileFromClasspathToInngaaende("__files/mailpiece/MP_RAPPORT_XML-VALID.xml");

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(inbound.resolve(FERDIG).resolve("MP_RAPPORT_XML-VALID.xml"))
					.exists().isRegularFile();
		});

	}

	@Test
	void shouldMoveFileToFeiletFolderWhenTypeIsInvalid() throws IOException {
		copyFileFromClasspathToInngaaende("__files/mailpiece/MP_RAPPORT_XML-INVALID-TYPE.xml");

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
				assertThat(inbound.resolve(FEILET).resolve("MP_RAPPORT_XML-INVALID-TYPE.xml"))
						.exists().isRegularFile()
		);
	}

	@Test
	void shouldMoveFileToFeiletFolderWhenLePuIsInvalid() throws IOException {
		copyFileFromClasspathToInngaaende("__files/mailpiece/MP_RAPPORT_XML-INVALID-LEPU.xml");

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
				assertThat(inbound.resolve(FEILET).resolve("MP_RAPPORT_XML-INVALID-LEPU.xml"))
						.exists().isRegularFile()
		);
	}

	private void copyFileFromClasspathToInngaaende(String klassepathFilnavn) throws IOException {
		ClassPathResource classPathResource = new ClassPathResource(klassepathFilnavn);
		Files.copy(classPathResource.getInputStream(), inbound.resolve(classPathResource.getFilename()));
	}
}
