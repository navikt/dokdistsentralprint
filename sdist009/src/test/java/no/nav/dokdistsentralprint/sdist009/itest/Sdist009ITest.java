package no.nav.dokdistsentralprint.sdist009.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistsentralprint.sdist009.itest.config.Sdist009TestConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.LogManager;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("itest")
@SpringBootTest(classes = Sdist009TestConfig.class,
		webEnvironment = RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@EnableWireMock()
class Sdist009ITest {

	private static final String FEILET = "feilet";
	private static final String FERDIG = "ferdig";
	private static final String INBOUND = "inbound/dokdistsentralprint";
	private static final String FORSENDELSE_ID = "33333";
	private static final String BESTILLINGS_ID = "MVA-P405-IN200C5-121127-0019";
	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/";
	private static final String FINNFORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/bestillingsId/";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String OPPDATER_FILINFO_URL = "/rest/v1/administrerforsendelse/oppdaterfilinformasjon";

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qopp001;

	@Autowired
	private Path sshdPath;

	private Path inbound;

	@BeforeEach
	void setup() {
		inbound = sshdPath.resolve(INBOUND);
		stubAzureToken();
	}

	@AfterEach
	void tearDown() throws IOException {
		FileUtils.cleanDirectory(inbound.toFile());
		LogManager.getLogManager().readConfiguration();
	}

	@ParameterizedTest
	@CsvSource({
			"MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml, hentforsendelse_status_oversendt.json",
			"MP_RAPPORT_XML-KONVOLUTTERT.xml, hentforsendelse_status_bekreftet.json",
			"MP_RAPPORT_XML-RETURPOST.xml, hentforsendelse_status_returpostbehandlet.json"
	})
	void shouldReadFileFromSftpAndBehandleKvitteringer(String mailpieceFilnavn, String forsendelseFilnavn) throws IOException {
		stubFinnForsendelse(OK);
		stubGetForsendelse(forsendelseFilnavn);
		stubPutFilInfo();
		stubPutOppdaterForsendelse();
		copyFileFromClasspathToInngaaende("__files/mailpiece/" + mailpieceFilnavn);

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(inbound.resolve(FERDIG).resolve(mailpieceFilnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void shouldReadLePuReturpostFromSftpAndWritesToQopp001Queue() throws IOException {
		stubFinnForsendelse(OK);
		stubGetForsendelse("hentforsendelse_status_ekspedert.json");
		stubPutFilInfo();
		stubPutOppdaterForsendelse();
		copyFileFromClasspathToInngaaende("__files/mailpiece/MP_RAPPORT_XML-RETURPOST.xml");

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			String receive = receive(qopp001);
			assertThat(receive).isNotNull();
			assertThat(receive).isEqualToIgnoringWhitespace(classpathToString("__files/qopp001/opp001-happy-melding.xml"));
			assertThat(inbound.resolve(FERDIG).resolve("MP_RAPPORT_XML-RETURPOST.xml"))
					.exists().isRegularFile();
		});
	}

	@Test
	void testNullRapport() throws IOException {
		stubFinnForsendelse(OK);
		stubGetForsendelse("hentforsendelse_status_ekspedert.json");
		stubPutFilInfo();
		stubPutOppdaterForsendelse();
		copyFileFromClasspathToInngaaende("__files/mailpiece/MP_RAPPORT_XML-INGEN-KVITTERINGER.xml");

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(inbound.resolve(FERDIG).resolve("MP_RAPPORT_XML-INGEN-KVITTERINGER.xml"))
					.exists().isRegularFile();
		});
	}

	@ParameterizedTest
	@MethodSource
	void shouldLogErrorWhenForsendelseStatusIsNotExpected(String mailpieceFilnavn, String forsendelseFilnavn, String message, CapturedOutput output) throws IOException {
		stubFinnForsendelse(OK);
		stubGetForsendelse(forsendelseFilnavn);
		stubPutFilInfo();
		stubPutOppdaterForsendelse();
		copyFileFromClasspathToInngaaende("__files/mailpiece/" + mailpieceFilnavn);

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(inbound.resolve(FERDIG).resolve(mailpieceFilnavn))
					.exists().isRegularFile();
			assertThat(output.getOut()).contains(message);
		});
	}

	private static Stream<Arguments> shouldLogErrorWhenForsendelseStatusIsNotExpected() {
		return Stream.of(
				Arguments.of("MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml", "hentforsendelse_status_returpostbehandlet.json", "Forventet dokumentStatus=[OVERSENDT, BEKREFTET]"),
				Arguments.of("MP_RAPPORT_XML-KONVOLUTTERT.xml", "hentforsendelse_status_oversendt.json", "Forventet dokumentStatus=[BEKREFTET, EKSPEDERT]"),
				Arguments.of("MP_RAPPORT_XML-RETURPOST.xml", "hentforsendelse_status_bekreftet.json", "Forventet dokumentStatus=[EKSPEDERT, RETURPOSTBEHANDLET]")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldLogInfoWhenForsendelseStatusIsUpdated(String mailpieceFilnavn, String forsendelseFilnavn, String message, CapturedOutput output) throws IOException {
		stubFinnForsendelse(OK);
		stubGetForsendelse(forsendelseFilnavn);
		stubPutFilInfo();
		stubPutOppdaterForsendelse();
		copyFileFromClasspathToInngaaende("__files/mailpiece/" + mailpieceFilnavn);

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(inbound.resolve(FERDIG).resolve(mailpieceFilnavn))
					.exists().isRegularFile();
			assertThat(output.getOut()).contains(message);
		});
	}

	private static Stream<Arguments> shouldLogInfoWhenForsendelseStatusIsUpdated() {
		return Stream.of(
				Arguments.of("MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml", "hentforsendelse_status_bekreftet.json", "er allerede bekreftet"),
				Arguments.of("MP_RAPPORT_XML-KONVOLUTTERT.xml", "hentforsendelse_status_ekspedert.json", "er allerede ekspedert"),
				Arguments.of("MP_RAPPORT_XML-RETURPOST.xml", "hentforsendelse_status_returpostbehandlet.json", "er allerede returpostbehandlet")
		);
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

	@Test
	void shouldThrowExceptionWhenBestillingIdIsNotFound() throws IOException {
		stubFinnForsendelse(NOT_FOUND);
		stubGetForsendelse("hentforsendelse_status_bekreftet.json");
		stubPutFilInfo();
		stubPutOppdaterForsendelse();
		copyFileFromClasspathToInngaaende("__files/mailpiece/MP_RAPPORT_XML-KONVOLUTTERT.xml");

		await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
				assertThat(inbound.resolve(FEILET).resolve("MP_RAPPORT_XML-KONVOLUTTERT.xml"))
						.exists().isRegularFile()
		);
	}

	private void copyFileFromClasspathToInngaaende(String klassepathFilnavn) throws IOException {
		ClassPathResource classPathResource = new ClassPathResource(klassepathFilnavn);
		Files.copy(classPathResource.getInputStream(), inbound.resolve(classPathResource.getFilename()));
	}

	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement<?>) response).getValue();
		}
		return (T) response;
	}

	void stubPutFilInfo() {
		stubFor(put(urlEqualTo(OPPDATER_FILINFO_URL))
				.willReturn(WireMock.aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokdistadmin/oppdaterfilinfo.json")));
	}

	void stubGetForsendelse(String filnavn) {
		stubFor(get(urlEqualTo(HENTFORSENDELSE_URL + FORSENDELSE_ID))
				.willReturn(WireMock.aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokdistadmin/" + filnavn)));
	}

	void stubPutOppdaterForsendelse() {
		stubFor(put(urlEqualTo(OPPDATERFORSENDELSE_URL))
				.willReturn(WireMock.aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	void stubFinnForsendelse(HttpStatus status) {
		stubFor(get(urlEqualTo(FINNFORSENDELSE_URL + BESTILLINGS_ID))
				.willReturn(WireMock.aResponse()
						.withStatus(status.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokdistadmin/finnforsendelse.json")));
	}

	void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/azure-token.json")));
	}

	public static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}
}
