package no.nav.dokdistsentralprint.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistsentralprint.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import no.nav.dokdistsentralprint.Application;
import no.nav.dokdistsentralprint.itest.config.ApplicationTestConfig;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import no.nav.dokdistsentralprint.storage.JsonSerializer;
import no.nav.dokdistsentralprint.storage.Storage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, ApplicationTestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@Disabled
public class Qdist009IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK = "dokumentObjektReferanseHoveddok";
	private static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 = "dokumentObjektReferanseVedlegg1";
	private static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 = "dokumentObjektReferanseVedlegg2";

	@Inject
	private JmsTemplate jmsTemplate;

	@Inject
	private Queue qdist009;

	@Inject
	private Queue qdist009FunksjonellFeil;

	@Inject
	private Queue backoutQueue;

	@Inject
	private Storage storage;

	@Inject
	public CacheManager cacheManager;

	@BeforeEach
	public void setupBefore() {
		DokdistDokument dokdistDokument = DokdistDokument.builder().pdf("HOVEDDOK_TEST".getBytes()).build();
		cacheManager.getCache(TKAT020_CACHE).clear();
		reset(storage);
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK)).thenReturn(Optional.of(JsonSerializer.serialize(DokdistDokument.builder()
				.pdf("HOVEDDOK_TEST".getBytes()).build())));
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1)).thenReturn(Optional.of(JsonSerializer.serialize(DokdistDokument.builder()
				.pdf("VEDLEGG1_TEST".getBytes()).build())));
		when(storage.get(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2)).thenReturn(Optional.of(JsonSerializer.serialize(DokdistDokument.builder()
				.pdf("VEDLEGG2_TEST".getBytes()).build())));
	}

	@Test
	public void shouldProcessForsendelse() throws Exception {
		DokdistDokument.builder().pdf("HOVEDDOK_TEST".getBytes()).build();
		stubFor(get(urlMatching("/dokkat-tkat020/dokumenttypeIdHoveddok")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/administrerforsendelse/v1/" + FORSENDELSE_ID)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/getForsendelse_noAdresse-happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));
		stubFor(post("/hentMottakerOgAdresse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("regoppslag/treg002-happy.json")));
		stubFor(post("/sts")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("sts/sts-happy.xml")));

		sendStringMessage(qdist009, classpathToString("qdist009/qdist009-happy.xml"));

		//todo assert ftp

//		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/dokumenttypeIdHoveddok")));
//
//		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
//				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutHappy.json"))));

	}


	private void sendStringMessage(Queue queue, final String message) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			msg.setText(message);
			return msg;
		});
	}

	private String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}


	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

	private String getRequestAsJson(String filename) throws IOException {

		File file = new ClassPathResource(filename).getFile();
		byte[] data = new byte[(int) file.length()];
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(data);
		fileInputStream.close();
		return new String(data);
	}
}



