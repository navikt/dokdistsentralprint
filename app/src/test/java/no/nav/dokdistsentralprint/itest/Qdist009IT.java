package no.nav.dokdistsentralprint.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistsentralprint.Application;
import no.nav.dokdistsentralprint.itest.config.ApplicationTestConfig;
import no.nav.dokdistsentralprint.storage.BucketStorage;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;
import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistsentralprint.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static no.nav.dokdistsentralprint.itest.config.SftpConfig.startSshServer;
import static no.nav.dokdistsentralprint.testUtils.classpathToString;
import static no.nav.dokdistsentralprint.testUtils.fileToString;
import static no.nav.dokdistsentralprint.testUtils.unzipToDirectory;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = Application.class, webEnvironment = NONE)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
class Qdist009IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK = "dokumentObjektReferanseHoveddok";
	private static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 = "dokumentObjektReferanseVedlegg1";
	private static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 = "dokumentObjektReferanseVedlegg2";
	private static final String HOVEDDOK_TEST_CONTENT = "HOVEDDOK_TEST_CONTENT";
	private static final String VEDLEGG1_TEST_CONTENT = "VEDLEGG1_TEST_CONTENT";
	private static final String VEDLEGG2_TEST_CONTENT = "VEDLEGG2_TEST_CONTENT";
	private static final String LANDKODE_TR = "TR";
	private static final String LANDKODE_XX = "XX";

	private static final String HENTFORSENDELSE_URL = String.format("/rest/v1/administrerforsendelse/%s", FORSENDELSE_ID);
	private static final String DOKMET_URL = "/rest/dokumenttypeinfo/dokumenttypeIdHoveddok";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String HENTPOSTDESTINASJON_URL = "/rest/v1/administrerforsendelse/hentpostdestinasjon/";
	private static final String OPPDATERPOSTADRESSE_URL = "/rest/v1/administrerforsendelse/oppdaterpostadresse";
	private static final String FEIL_REGISTRER_FORSENDELSE_URL = "/rest/v1/administrerforsendelse/feilregistrerforsendelse";


	@TempDir
	static Path tempDir;
	private static String CALL_ID;
	private static SshServer sshServer;
	@Autowired
	public CacheManager cacheManager;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private Queue qdist009;
	@Autowired
	private Queue qdist009FunksjonellFeil;
	@Autowired
	private Queue backoutQueue;
	@Autowired
	private Queue qdokopp001;
	@Autowired
	private BucketStorage bucketStorage;

	@Import(ApplicationTestConfig.class)
	@Configuration
	static class Config {

	}

	@DynamicPropertySource
	static void registerSftpProperties(DynamicPropertyRegistry registry) {
		sshServer = startSshServer(tempDir);
		registry.add("sftp.privateKeyFile", () -> {
			try {
				return new ClassPathResource("ssh/id_rsa").getURL().getPath();
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		});
		registry.add("sftp.port", () -> sshServer.getPort());
	}

	@AfterAll
	public static void stopServer() throws Exception {
		sshServer.stop(true);
	}

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();

		cacheManager.getCache(TKAT020_CACHE).clear();
		reset(bucketStorage);
		when(bucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK), anyString())).thenReturn(JsonSerializer.serialize(DokdistDokument.builder().pdf(HOVEDDOK_TEST_CONTENT.getBytes()).build()));
		when(bucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1), anyString())).thenReturn(JsonSerializer.serialize(DokdistDokument.builder().pdf(VEDLEGG1_TEST_CONTENT.getBytes()).build()));
		when(bucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2), anyString())).thenReturn(JsonSerializer.serialize(DokdistDokument.builder().pdf(VEDLEGG2_TEST_CONTENT.getBytes()).build()));

		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	@Test
	void shouldProcessForsendelse() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistsentralprint/" + CALL_ID + ".zip";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));
		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());

		String bestillingXmlPath = tempDir.toString() + "/" + CALL_ID + ".xml";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(bestillingXmlPath).exists())); // Test sometimes get FileNotFound. This check prevents it
		String actualBestillingXmlString = fileToString(new File(bestillingXmlPath));
		String expectedBestillingXmlString = classpathToString("/qdist009/bestilling_xml.xml").replaceAll("insertCallIdHere",
				CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verifyAllStubs(LANDKODE_TR);
	}

	@Test
	void shouldProcessForsendelseMedUkjentEllerNullLandkode() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());
		stubGetPostdestinasjon("XX", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy-landkode-ukjent.json", OK.value());
		stubPutPostadresse(OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistsentralprint/" + CALL_ID + ".zip";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));
		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());

		String bestillingXmlPath = tempDir.toString() + "/" + CALL_ID + ".xml";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(bestillingXmlPath).exists())); // Test sometimes get FileNotFound. This check prevents it
		String actualBestillingXmlString = fileToString(new File(bestillingXmlPath));
		String expectedBestillingXmlString = classpathToString("/qdist009/bestilling_ukjent_landkode.xml").replaceAll("insertCallIdHere",
				CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verifyAllStubs(LANDKODE_XX);
	}

	@Test
	void shouldSendTemaRequestTilRegoppslagOgProcessForsendelse() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/forsendelse_med_tema.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistsentralprint/" + CALL_ID + ".zip";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));
		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());

		String bestillingXmlPath = tempDir.toString() + "/" + CALL_ID + ".xml";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(bestillingXmlPath).exists())); // Test sometimes get FileNotFound. This check prevents it
		String actualBestillingXmlString = fileToString(new File(bestillingXmlPath));
		String expectedBestillingXmlString = classpathToString("/qdist009/bestilling_xml.xml").replaceAll("insertCallIdHere",
				CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verifyAllStubs(LANDKODE_TR);
	}

	@Test
	void shouldProcessForsendelseWithoutCallingRegoppslag() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_withAdresse-happy.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());
		stubGetPostdestinasjon("NO", "rdist001/hentPostdestinasjon-happy.json", OK.value());

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistsentralprint/" + CALL_ID + ".zip";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));
		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());

		String bestillingXmlPath = tempDir.toString() + "/" + CALL_ID + ".xml";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(bestillingXmlPath).exists())); // Test sometimes get FileNotFound. This check prevents it
		String actualBestillingXmlString = fileToString(new File(bestillingXmlPath));
		String expectedBestillingXmlString = classpathToString("/qdist009/bestilling_utenRegoppslag_xml.xml").replaceAll(
				"insertCallIdHere",
				CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verify(1, getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTPOSTDESTINASJON_URL + "NO")));
	}

	@Test
	void shouldProcessForsendelseWithoutInkludertSkatteyternummerIXml() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_withUkjent_motakertype-happy.json", OK.value());
		stubPutOppdaterForsendelse(OK.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		String zippedFilePath = tempDir.toString() + "/outbound/dokdistsentralprint/" + CALL_ID + ".zip";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));
		unzipToDirectory(zippedFilePath, new File(tempDir.toString()).toPath());

		String bestillingXmlPath = tempDir.toString() + "/" + CALL_ID + ".xml";
		await().atMost(10, SECONDS).untilAsserted(() -> assertTrue(new File(bestillingXmlPath).exists())); // Test sometimes get FileNotFound. This check prevents it
		String actualBestillingXmlString = fileToString(new File(bestillingXmlPath));
		String expectedBestillingXmlString = classpathToString("/qdist009/bestilling_xml_uten_skattyternummer.xml").replaceAll("insertCallIdHere",
				CALL_ID);
		String hoveddokContent = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK + ".pdf"));
		String vedlegg1Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 + ".pdf"));
		String vedlegg2Content = fileToString(new File(tempDir.toString() + "/" + DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 + ".pdf"));

		assertEquals(expectedBestillingXmlString, actualBestillingXmlString.replaceAll("<KundeOpprettet.*KundeOpprettet>", ""));
		assertEquals(HOVEDDOK_TEST_CONTENT, hoveddokContent);
		assertEquals(VEDLEGG1_TEST_CONTENT, vedlegg1Content);
		assertEquals(VEDLEGG2_TEST_CONTENT, vedlegg2Content);

		verifyAllStubs(LANDKODE_TR);
	}

	@Test
	void shouldSendMeldingToQdokopp001QueueWhenPostadresseIsNull() throws IOException {
		stubRestSts();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubFeilPostHentMottakerOgAdresse(NOT_FOUND.value());
		stubPutFeilregistrerForsendelse();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(100, SECONDS).untilAsserted(() -> {
			String qdokopp001Receive = receive(qdokopp001);
			assertNotNull(qdokopp001Receive);
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
		verify(1, putRequestedFor(urlEqualTo(FEIL_REGISTRER_FORSENDELSE_URL)));
	}


	@Test
	void shouldThrowForsendelseManglerForsendelseIdFunctionalExceptionManglerForsendelseId() throws Exception {
		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-feilId.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-feilId.xml"));
		});
	}

	@Test
	void shouldThrowForsendelseManglerForsendelseIdFunctionalExceptionTomForsendelseId() throws Exception {
		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-tom-forsendelseId.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-tom-forsendelseId.xml"));
		});
	}

	@Test
	void shouldThrowRdist001HentForsendelseFunctionalException() throws Exception {

		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse().withStatus(NOT_FOUND.value())));

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
	}

	@Test
	void shouldThrowRdist001HentForsendelseTechnicalException() throws Exception {
		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist009BackoutQueue);
			assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
	}

	@Test
	void shouldThrowInvalidForsendelseStatusException() throws Exception {
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresseBekreftetForsendelseStatus.json", OK.value());

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
	}

	@Test
	void shouldThrowTkat020FunctionalException() throws Exception {
		stubFor(get(urlMatching(DOKMET_URL))
				.willReturn(aResponse().withStatus(NOT_FOUND.value())));
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubRestSts();
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());


		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(DOKMET_URL)));
	}

	@Test
	void shouldThrowTkat020TechicalException() throws Exception {
		stubFor(get(urlMatching(DOKMET_URL))
				.willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubRestSts();
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());


		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist009BackoutQueue);
			assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
	}

	@Test
	void shouldThrowRegoppslagHentAdresseFunctionalException() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubRestSts();
		stubPostHentMottakerOgAdresse(null, NOT_FOUND.value());


		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	@Test
	void shouldThrowRegoppslagHentAdresseTechnicalException() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", INTERNAL_SERVER_ERROR.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist009BackoutQueue);
			assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(MAX_ATTEMPTS_SHORT, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	@Test
	void shouldThrowRdist001GetPostDestinasjonFunctionalException() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubGetPostdestinasjon("TR", null, NOT_FOUND.value());
		stubPutPostadresse(OK.value());

		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTPOSTDESTINASJON_URL + "TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	@Test
	void shouldThrowRdist001GetPostDestinasjonTechnicalException() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubGetPostdestinasjon("TR", null, INTERNAL_SERVER_ERROR.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist009BackoutQueue);
			assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo(HENTPOSTDESTINASJON_URL + "TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	@Test
	void shouldThrowKunneIkkeDeserialisereBucketPayloadFunctionalException() throws Exception {
		when(bucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2), anyString())).thenReturn("notJsonSerializedString");

		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_withAdresse-CorruptInBucket-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTPOSTDESTINASJON_URL + "TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	@Test
	void shouldThrowDokumentIkkeFunnetIBucketException() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_withAdresse-NotInBucket-happy.json", OK.value());
		stubGetPostdestinasjon("NO", "rdist001/hentPostdestinasjon-happy.json", OK.value());

		stubRestSts();
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verify(1, getRequestedFor(urlEqualTo("/rest/dokumenttypeinfo/dokumenttypeIdHoveddokNotInBucket")));
		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
	}

	@Test
	void shouldPutMessageOnFunksjonellFeilkoeIfNotFoundFromRdist001() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPutOppdaterForsendelse(NOT_FOUND.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
			assertNotNull(resultOnQdist009FunksjonellFeilQueue);
			assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});

		verifyAllStubs(LANDKODE_TR);
	}

	@Test
	void shouldPutMessageOnBackoutkoeIfInternalServerErrorFromRdist001() throws Exception {
		stubGetDokumenttype();
		stubGetForsendelse("__files/rdist001/getForsendelse_noAdresse-happy.json", OK.value());
		stubPutOppdaterForsendelse(INTERNAL_SERVER_ERROR.value());
		stubGetPostdestinasjon("TR", "rdist001/hentPostdestinasjon-happy.json", OK.value());
		stubPostHentMottakerOgAdresse("regoppslag/treg002-happy.json", OK.value());
		stubPutPostadresse(OK.value());
		stubRestSts();

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist009BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist009BackoutQueue);
			assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
		});
		verify(1, getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(MAX_ATTEMPTS_SHORT,
				putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTPOSTDESTINASJON_URL + "TR")));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	private void sendStringMessage(Queue queue, final String message) {
		jmsTemplate.send(queue, session -> {
			TextMessage textMessage = session.createTextMessage();
			textMessage.setText(message);
			textMessage.setStringProperty("callId", CALL_ID);
			return textMessage;
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement<?>) response).getValue();
		}
		return (T) response;
	}

	private void verifyAllStubs(String landkode) {
		verify(1, getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1,
				putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(HENTPOSTDESTINASJON_URL + landkode)));
		verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
	}

	private void stubRestSts() {
		stubFor(get("/reststs/token?grant_type=client_credentials&scope=openid")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("reststs/rest_sts_happy.json")));
	}

	private void stubPutOppdaterForsendelse(int status) {
		stubFor(put(OPPDATERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(status)));
	}

	private void stubGetForsendelse(String path, int status) throws IOException {
		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(path).replace(
								"insertCallIdHere",
								CALL_ID))));
	}

	private void stubPutFeilregistrerForsendelse() {
		stubFor(put(urlEqualTo(FEIL_REGISTRER_FORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubPutPostadresse(int status) {
		stubFor(put(OPPDATERPOSTADRESSE_URL)
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	private void stubGetPostdestinasjon(String landkode, String path, int status) {
		stubFor(get(HENTPOSTDESTINASJON_URL + landkode)
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	private void stubGetDokumenttype() {
		stubFor(get(urlMatching(DOKMET_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokumentinfov4/tkat020-happy.json")));
	}

	private void stubPostHentMottakerOgAdresse(String path, int status) {
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	private void stubFeilPostHentMottakerOgAdresse(int status) {
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("{\"status\":\"404 \",\"message\":\"Fant ikke adresse for personen i PDL\"}")));
	}
}



