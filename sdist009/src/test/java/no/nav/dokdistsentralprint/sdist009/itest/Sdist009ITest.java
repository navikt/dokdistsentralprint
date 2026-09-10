package no.nav.dokdistsentralprint.sdist009.itest;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistsentralprint.sdist009.ForsendelseStatus;
import no.nav.dokdistsentralprint.sdist009.itest.config.Sdist009TestConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import java.util.logging.LogManager;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.FEILET;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.OPPRETTET;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.OVERSENDT;
import static no.nav.dokdistsentralprint.sdist009.ForsendelseStatus.RETURPOSTBEHANDLET;
import static no.nav.dokdistsentralprint.sdist009.Sdist009Service.FORVENTET_KONVOLUTTERT_STATUS;
import static no.nav.dokdistsentralprint.sdist009.Sdist009Service.FORVENTET_MAILPIECE_MOTTAK_STATUS;
import static no.nav.dokdistsentralprint.sdist009.Sdist009Service.FORVENTET_RETURPOST_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ActiveProfiles("itest")
@SpringBootTest(classes = Sdist009TestConfig.class,
		webEnvironment = RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@EnableWireMock
class Sdist009ITest {

	private static final String FORSENDELSE_ID = "33333";
	private static final String BESTILLINGS_ID = "MVA-P405-IN200C5-121127-0019";
	private static final String HENT_FORSENDELSE_URL = "/rest/v1/administrerforsendelse/";
	private static final String FINN_FORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/bestillingsId/";
	private static final String OPPDATER_FORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String OPPDATER_FILINFORMASJON_URL = "/rest/v1/administrerforsendelse/oppdaterfilinformasjon";

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qopp001;

	@Autowired
	private Path sshdPath;

	private Path inbound;
	private Path ferdig;
	private Path feilet;

	@BeforeEach
	void setup() {
		inbound = sshdPath.resolve("inbound/dokdistsentralprint");
		ferdig = inbound.resolve("ferdig");
		feilet = inbound.resolve("feilet");

		stubAzureToken();
	}

	@AfterEach
	void tearDown() throws IOException {
		FileUtils.cleanDirectory(inbound.toFile());
		LogManager.getLogManager().readConfiguration();
	}

	@Test
	void skalBehandleKvitteringMedLePuMailpieceMottakOgStatusOversendt() throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_oversendt.json");
		stubOppdaterFilinformasjon();
		stubOppdaterForsendelse();

		String filnavn = "MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(10, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalBehandleKvitteringMedLePuMailpieceMottakOgStatusBekreftet(CapturedOutput capturedOutput) throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_bekreftet.json");
		stubOppdaterFilinformasjon();

		String filnavn = "MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(capturedOutput.getOut()).contains("Forsendelse med forsendelseId=33333, LePu=MAILPIECE_MOTTAK og dokumentstatus=BEKREFTET er allerede bekreftet. Avslutter behandling av kvittering og går til neste.");

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	// Kun OVERSENDT og BEKREFTET er forventede statuser på forsendelse for LePu=Mailpiece_mottak
	@ParameterizedTest
	@MethodSource
	void skalBehandleKvitteringMedLePuMailpieceMottakOgUforventetStatus(String filnavn, ForsendelseStatus forsendelseStatus, CapturedOutput output) throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse(filnavn);
		stubOppdaterFilinformasjon();

		String kvitteringsfilnavn = "MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml";
		kopierFilTilInngaaende(kvitteringsfilnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(output.getOut()).contains(
					"Forsendelse med forsendelseId=33333 og LePu=MAILPIECE_MOTTAK har dokumentstatus=%s. Forventet dokumentstatus=%s. Avslutter behandling av kvittering og går til neste."
							.formatted(forsendelseStatus, FORVENTET_MAILPIECE_MOTTAK_STATUS)
			);

			assertThat(ferdig.resolve(kvitteringsfilnavn))
					.exists().isRegularFile();
		});
	}

	private static Stream<Arguments> skalBehandleKvitteringMedLePuMailpieceMottakOgUforventetStatus() {
		return Stream.of(
				Arguments.of("hentforsendelse_status_opprettet.json", OPPRETTET),
				Arguments.of("hentforsendelse_status_klarfordist.json", KLAR_FOR_DIST),
				Arguments.of("hentforsendelse_status_feilet.json", FEILET),
				Arguments.of("hentforsendelse_status_ekspedert.json", EKSPEDERT),
				Arguments.of("hentforsendelse_status_returpostbehandlet.json", RETURPOSTBEHANDLET)
		);
	}

	@Test
	void skalBehandleKvitteringMedLePuKonvoluttertOgStatusBekreftet() throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_bekreftet.json");
		stubOppdaterFilinformasjon();
		stubOppdaterForsendelse();

		String filnavn = "MP_RAPPORT_XML-KONVOLUTTERT.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(10, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalBehandleKvitteringMedLePuKonvoluttertOgStatusEkspedert(CapturedOutput capturedOutput) throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_ekspedert.json");
		stubOppdaterFilinformasjon();

		String filnavn = "MP_RAPPORT_XML-KONVOLUTTERT.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(capturedOutput.getOut()).contains("Forsendelse med forsendelseId=33333, LePu=KONVOLUTTERT og dokumentstatus=EKSPEDERT er allerede ekspedert. Avslutter behandling av kvittering og går til neste.");

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	// Kun BEKREFTET og EKSPEDERT er forventede statuser på forsendelse for LePu=Konvoluttert
	@ParameterizedTest
	@MethodSource
	void skalBehandleKvitteringMedLePuKonvoluttertOgUforventetStatus(String filnavn, ForsendelseStatus forsendelseStatus, CapturedOutput output) throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse(filnavn);
		stubOppdaterFilinformasjon();

		String kvitteringsfilnavn = "MP_RAPPORT_XML-KONVOLUTTERT.xml";
		kopierFilTilInngaaende(kvitteringsfilnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(output.getOut()).contains(
					"Forsendelse med forsendelseId=33333 og LePu=KONVOLUTTERT har dokumentstatus=%s. Forventet dokumentstatus=%s. Avslutter behandling av kvittering og går til neste."
							.formatted(forsendelseStatus, FORVENTET_KONVOLUTTERT_STATUS)
			);

			assertThat(ferdig.resolve(kvitteringsfilnavn))
					.exists().isRegularFile();
		});
	}

	private static Stream<Arguments> skalBehandleKvitteringMedLePuKonvoluttertOgUforventetStatus() {
		return Stream.of(
				Arguments.of("hentforsendelse_status_opprettet.json", OPPRETTET),
				Arguments.of("hentforsendelse_status_klarfordist.json", KLAR_FOR_DIST),
				Arguments.of("hentforsendelse_status_oversendt.json", OVERSENDT),
				Arguments.of("hentforsendelse_status_feilet.json", FEILET),
				Arguments.of("hentforsendelse_status_returpostbehandlet.json", RETURPOSTBEHANDLET)
		);
	}

	@Test
	void skalBehandleKvitteringMedLePuReturpostbehandletOgStatusEkspedert() throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_ekspedert.json");
		stubOppdaterFilinformasjon();
		stubOppdaterForsendelse();

		String filnavn = "MP_RAPPORT_XML-RETURPOST.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(10, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			String receive = receive(qopp001);
			assertThat(receive).isNotNull();
			assertThat(receive).isEqualToIgnoringWhitespace(classpathToString("__files/qopp001/opp001-happy-melding.xml"));

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalBehandleKvitteringMedLePuReturpostOgStatusReturpostbehandlet(CapturedOutput capturedOutput) throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_returpostbehandlet.json");
		stubOppdaterFilinformasjon();

		String filnavn = "MP_RAPPORT_XML-RETURPOST.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(capturedOutput.getOut()).contains("Forsendelse med forsendelseId=33333, LePu=RETURPOST og dokumentstatus=RETURPOSTBEHANDLET er allerede returpostbehandlet. Avslutter behandling av kvittering og går til neste.");

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	// Kun EKSPEDERT og RETURPOSTBEHANDLET er forventede statuser på forsendelse for LePu=Returpost
	@ParameterizedTest
	@MethodSource
	void skalBehandleKvitteringMedLePuReturpostOgUforventetStatus(String filnavn, ForsendelseStatus forsendelseStatus, CapturedOutput output) throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse(filnavn);
		stubOppdaterFilinformasjon();

		String kvitteringsfilnavn = "MP_RAPPORT_XML-RETURPOST.xml";
		kopierFilTilInngaaende(kvitteringsfilnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(output.getOut()).contains(
					"Forsendelse med forsendelseId=33333 og LePu=RETURPOST har dokumentstatus=%s. Forventet dokumentstatus=%s. Avslutter behandling av kvittering og går til neste."
							.formatted(forsendelseStatus, FORVENTET_RETURPOST_STATUS)
			);

			assertThat(ferdig.resolve(kvitteringsfilnavn))
					.exists().isRegularFile();
		});
	}

	private static Stream<Arguments> skalBehandleKvitteringMedLePuReturpostOgUforventetStatus() {
		return Stream.of(
				Arguments.of("hentforsendelse_status_opprettet.json", OPPRETTET),
				Arguments.of("hentforsendelse_status_klarfordist.json", KLAR_FOR_DIST),
				Arguments.of("hentforsendelse_status_oversendt.json", OVERSENDT),
				Arguments.of("hentforsendelse_status_bekreftet.json", BEKREFTET),
				Arguments.of("hentforsendelse_status_feilet.json", FEILET)
		);
	}

	@Test
	void skalFlytteTilFeilmappeHvisKvitteringsfilHarNullKvitteringer() throws IOException {
		String filnavn = "MP_RAPPORT_XML-INGEN-KVITTERINGER.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			assertThat(feilet.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalKunOppdatereForsendelsestatusDersomReturpostHarUgyldigArkivinformasjon() throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_ekspedert_med_ugyldig_arkivinformasjon.json");
		stubOppdaterFilinformasjon();
		stubOppdaterForsendelse();

		String filnavn = "MP_RAPPORT_XML-RETURPOST.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(10, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisTypeIkkeErMailpiece() throws IOException {
		String filnavn = "MP_RAPPORT_XML-UGYLDIG-TYPE.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() ->
				assertThat(feilet.resolve(filnavn))
						.exists().isRegularFile()
		);
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisLePuErUgyldig() throws IOException {
		String filnavn = "MP_RAPPORT_XML-UGYLDIG-LEPU.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() ->
				assertThat(feilet.resolve(filnavn))
						.exists().isRegularFile()
		);
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisForsendelsestatusErUkjent() throws IOException {
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_ukjent.json");
		stubOppdaterFilinformasjon();

		String filnavn = "MP_RAPPORT_XML-MAILPIECE_MOTTAK.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, putRequestedFor(urlEqualTo(OPPDATER_FORSENDELSE_URL)));
			verify(1, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			assertThat(feilet.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisOppdaterFilinformasjonReturnerInternalServerError() throws IOException {
		stubOppdaterFilinformasjon(INTERNAL_SERVER_ERROR);

		String filnavn = "MP_RAPPORT_XML-ALLE_LEPUVERDIER.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(4, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(feilet.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalFlytteKvitteringsfilTilFerdigHvisKvitteringerHarStatusFeilet(CapturedOutput capturedOutput) throws IOException {
		stubOppdaterFilinformasjon();
		stubFinnForsendelse(OK);
		stubHentForsendelse("hentforsendelse_status_klarfordist.json");

		String filnavn = "MP_RAPPORT_XML-KVITTERINGER_MED_STATUS_FEILET.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(0, getRequestedFor(urlEqualTo(FINN_FORSENDELSE_URL + BESTILLINGS_ID)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));
			assertThat(capturedOutput.getOut()).contains("Kvittering med bestillingsId=MVA-P405-IN200C5-121127-0019 og statuskode=FEIL har feilet. Avslutter behandling av kvittering og går til neste.");

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalHoppeOverKvitteringerDerFinnForsendelseReturnerNotFound() throws IOException {
		stubOppdaterFilinformasjon();
		stubFinnForsendelse(NOT_FOUND);

		String filnavn = "MP_RAPPORT_XML-ALLE_LEPUVERDIER.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(3, getRequestedFor(urlEqualTo(FINN_FORSENDELSE_URL + BESTILLINGS_ID)));
			verify(0, getRequestedFor(urlEqualTo(HENT_FORSENDELSE_URL + FORSENDELSE_ID)));
			verify(2, putRequestedFor(urlEqualTo(OPPDATER_FILINFORMASJON_URL)));

			assertThat(ferdig.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisFinnForsendelseReturnerInternalServerError() throws IOException {
		stubOppdaterFilinformasjon();
		stubFinnForsendelse(INTERNAL_SERVER_ERROR);

		String filnavn = "MP_RAPPORT_XML-ALLE_LEPUVERDIER.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(4, getRequestedFor(urlEqualTo(FINN_FORSENDELSE_URL + BESTILLINGS_ID)));
			assertThat(feilet.resolve(filnavn))
					.exists().isRegularFile();
		});
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisHentForsendelseReturnerNotFound() throws IOException {
		stubOppdaterFilinformasjon();
		stubFinnForsendelse(OK);
		stubHentForsendelse(NOT_FOUND);

		String filnavn = "MP_RAPPORT_XML-ALLE_LEPUVERDIER.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
					verify(1, getRequestedFor(urlEqualTo(FINN_FORSENDELSE_URL + BESTILLINGS_ID)));
					verify(1, getRequestedFor(urlEqualTo(HENT_FORSENDELSE_URL + FORSENDELSE_ID)));
					assertThat(feilet.resolve(filnavn))
							.exists().isRegularFile();
				}
		);
	}

	@Test
	void skalFlytteKvitteringsfilTilFeiletHvisHentForsendelseReturnererInternalServerError() throws IOException {
		stubOppdaterFilinformasjon();
		stubFinnForsendelse(OK);
		stubHentForsendelse(INTERNAL_SERVER_ERROR);

		String filnavn = "MP_RAPPORT_XML-ALLE_LEPUVERDIER.xml";
		kopierFilTilInngaaende(filnavn);

		await().atMost(10, SECONDS).untilAsserted(() -> {
					verify(1, getRequestedFor(urlEqualTo(FINN_FORSENDELSE_URL + BESTILLINGS_ID)));
					verify(4, getRequestedFor(urlEqualTo(HENT_FORSENDELSE_URL + FORSENDELSE_ID)));
					assertThat(feilet.resolve(filnavn))
							.exists().isRegularFile();
				}
		);
	}

	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement<?>) response).getValue();
		}
		return (T) response;
	}

	void stubOppdaterFilinformasjon() {
		stubFor(put(urlEqualTo(OPPDATER_FILINFORMASJON_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokdistadmin/oppdaterfilinfo.json")));
	}

	void stubOppdaterFilinformasjon(HttpStatus httpStatus) {
		stubFor(put(urlEqualTo(OPPDATER_FILINFORMASJON_URL))
				.willReturn(aResponse()
						.withStatus(httpStatus.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	void stubFinnForsendelse(HttpStatus status) {
		stubFor(get(urlEqualTo(FINN_FORSENDELSE_URL + BESTILLINGS_ID))
				.willReturn(aResponse()
						.withStatus(status.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokdistadmin/finnforsendelse.json")));
	}

	void stubHentForsendelse(String filnavn) {
		stubFor(get(urlEqualTo(HENT_FORSENDELSE_URL + FORSENDELSE_ID))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokdistadmin/" + filnavn)));
	}

	void stubHentForsendelse(HttpStatus status) {
		stubFor(get(urlEqualTo(HENT_FORSENDELSE_URL + FORSENDELSE_ID))
				.willReturn(aResponse()
						.withStatus(status.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	void stubOppdaterForsendelse() {
		stubFor(put(urlEqualTo(OPPDATER_FORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/azure-token.json")));
	}

	private void kopierFilTilInngaaende(String filnavn) throws IOException {
		ClassPathResource classPathResource = new ClassPathResource("__files/mailpiece/" + filnavn);
		Files.copy(classPathResource.getInputStream(), inbound.resolve(filnavn));
	}

	private String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}

}