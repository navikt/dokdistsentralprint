package no.nav.dokdistsentralprint.itest;

import com.amazonaws.services.s3.AmazonS3;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistsentralprint.Application;
import no.nav.dokdistsentralprint.itest.config.ApplicationTestConfig;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.http.entity.ContentType;
import org.apache.sshd.server.SshServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
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
import static no.nav.dokdistsentralprint.storage.S3Configuration.BUCKET_NAME;
import static no.nav.dokdistsentralprint.testUtils.classpathToString;
import static no.nav.dokdistsentralprint.testUtils.fileToString;
import static no.nav.dokdistsentralprint.testUtils.unzipToDirectory;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, ApplicationTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    @TempDir
    static Path tempDir;
    private static String CALL_ID;
    private static SshServer sshServer;
    @Inject
    public CacheManager cacheManager;
    @Inject
    private JmsTemplate jmsTemplate;
    @Inject
    private Queue qdist009;
    @Inject
    private Queue qdist009FunksjonellFeil;
    @Inject
    private Queue backoutQueue;
    @Inject
    private AmazonS3 amazonS3;

    @BeforeAll
    public static void setupBeforeAll() throws IOException {
        sshServer = startSshServer(tempDir);
        System.setProperty("sftp.privateKeyFile", new ClassPathResource("ssh/id_rsa").getURL().getPath());
        System.setProperty("sftp.port", Integer.toString(sshServer.getPort()));
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
        reset(amazonS3);
        when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK))).thenReturn(JsonSerializer.serialize(DokdistDokument.builder().pdf(HOVEDDOK_TEST_CONTENT.getBytes()).build()));
        when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1))).thenReturn(JsonSerializer.serialize(DokdistDokument.builder().pdf(VEDLEGG1_TEST_CONTENT.getBytes()).build()));
        when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2))).thenReturn(JsonSerializer.serialize(DokdistDokument.builder().pdf(VEDLEGG2_TEST_CONTENT.getBytes()).build()));
    }

    @Test
    void shouldProcessForsendelse() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace("insertCallIdHere", CALL_ID))));
        stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
                .willReturn(aResponse().withStatus(OK.value())));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
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

        verifyAllStubs();
    }

    @Test
    void shouldSendTemaRequestTilRegoppslagOgProcessForsendelse() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/forsendelse_med_tema.json").replace("insertCallIdHere", CALL_ID))));
        stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
                .willReturn(aResponse().withStatus(OK.value())));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        String zippedFilePath = tempDir.toString() + "/outbound/dokdistsentralprint/" + CALL_ID + ".zip";
        await().atMost(100, SECONDS).untilAsserted(() -> assertTrue(new File(zippedFilePath).exists()));
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

        verifyAllStubs();
    }

    @Test
    void shouldProcessForsendelseWithoutCallingRegoppslag() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
                .willReturn(aResponse().withStatus(OK.value())));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/NO")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));

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

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1,
                putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/NO")));
    }


    @Test
    void shouldProcessForsendelseWithoutInkludertSkatteyternummerIXml() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_withUkjent_motakertype-happy.json")
                                .replace("insertCallIdHere", CALL_ID))));
        stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
                .willReturn(aResponse().withStatus(OK.value())));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
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

        verifyAllStubs();
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

        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(NOT_FOUND.value())));

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
    }

    @Test
    void shouldThrowRdist001HentForsendelseTechnicalException() throws Exception {
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009BackoutQueue = receive(backoutQueue);
            assertNotNull(resultOnQdist009BackoutQueue);
            assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
    }

    @Test
    void shouldThrowInvalidForsendelseStatusException() throws Exception {
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresseBekreftetForsendelseStatus.json")
                                .replace("insertCallIdHere", CALL_ID))));

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
    }

    @Test
    void shouldThrowTkat020FunctionalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok"))
                .willReturn(aResponse().withStatus(NOT_FOUND.value())));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
    }

    @Test
    void shouldThrowTkat020TechicalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009BackoutQueue = receive(backoutQueue);
            assertNotNull(resultOnQdist009BackoutQueue);
            assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
    }

    @Test
    void shouldThrowRegoppslagHentAdresseFunctionalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(NOT_FOUND.value())));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    @Test
    void shouldThrowRegoppslagHentAdresseTechnicalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009BackoutQueue = receive(backoutQueue);
            assertNotNull(resultOnQdist009BackoutQueue);
            assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(MAX_ATTEMPTS_SHORT, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    @Test
    void shouldThrowRdist001GetPostDestinasjonFunctionalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(NOT_FOUND.value())));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
        verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    @Test
    void shouldThrowRdist001GetPostDestinasjonTechnicalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009BackoutQueue = receive(backoutQueue);
            assertNotNull(resultOnQdist009BackoutQueue);
            assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(MAX_ATTEMPTS_SHORT, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
        verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    @Test
    void shouldThrowKunneIkkeDeserialisereS3PayloadFunctionalException() throws Exception {
        when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2))).thenReturn("notJsonSerializedString");

        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-CorruptInS3-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
        stubRestSts();
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
        verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    @Test
    void shouldThrowDokumentIkkeFunnetIS3Exception() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddokNotInS3")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_withAdresse-NotInS3-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/NO")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(NOT_FOUND.value())));
        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddokNotInS3")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/NO")));
    }

    @Test
    void shouldThrowRdist001OppdaterForsendelseStatusFunctionalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
                .willReturn(aResponse().withStatus(NOT_FOUND.value())));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009FunksjonellFeilQueue = receive(qdist009FunksjonellFeil);
            assertNotNull(resultOnQdist009FunksjonellFeilQueue);
            assertEquals(resultOnQdist009FunksjonellFeilQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });

        verifyAllStubs();
    }

    @Test
    void shouldThrowRdist001OppdaterForsendelseStatusTechnicalException() throws Exception {
        stubFor(get(urlMatching("/dokkat/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(OK.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .withBodyFile("dokumentinfov4/tkat020-happy.json")));
        stubFor(get("/administrerforsendelse/" + FORSENDELSE_ID)
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(classpathToString("__files/rjoark001/getForsendelse_noAdresse-happy.json").replace(
                                "insertCallIdHere",
                                CALL_ID))));
        stubFor(put("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
        stubFor(get("/administrerforsendelse/hentpostdestinasjon/TR")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBodyFile("rjoark001/getPostDestinasjon-happy.json")));
        stubFor(post("/hentMottakerOgAdresse")
                .willReturn(aResponse().withStatus(OK.value())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                        .withBodyFile("regoppslag/treg002-happy.json")));
        stubRestSts();

        sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

        await().atMost(10, SECONDS).untilAsserted(() -> {
            String resultOnQdist009BackoutQueue = receive(backoutQueue);
            assertNotNull(resultOnQdist009BackoutQueue);
            assertEquals(resultOnQdist009BackoutQueue, classpathToString("qdist009/qdist009-happy.xml"));
        });
        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(MAX_ATTEMPTS_SHORT,
                putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
        verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    private void sendStringMessage(Queue queue, final String message) {
        jmsTemplate.send(queue, session -> {
            TextMessage msg = new ActiveMQTextMessage();
            msg.setText(message);
            msg.setStringProperty("callId", CALL_ID);
            return msg;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T receive(Queue queue) {
        Object response = jmsTemplate.receiveAndConvert(queue);
        if (response instanceof JAXBElement) {
            response = ((JAXBElement) response).getValue();
        }
        return (T) response;
    }

    private void verifyAllStubs() {
        verify(1, getRequestedFor(urlEqualTo("/dokkat/dokumenttypeIdHoveddok")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
        verify(1,
                putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")));
        verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/hentpostdestinasjon/TR")));
        verify(1, postRequestedFor(urlEqualTo("/hentMottakerOgAdresse")));
    }

    private void stubRestSts() {
        stubFor(get("/reststs/token?grant_type=client_credentials&scope=openid")
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("reststs/rest_sts_happy.json")));
    }
}



