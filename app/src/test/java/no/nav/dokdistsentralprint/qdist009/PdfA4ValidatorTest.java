package no.nav.dokdistsentralprint.qdist009;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static no.nav.dokdistsentralprint.qdist009.PdfA4Validator.loggHvisDetFinnesPagesSomErStoerreEnnA4;
import static org.assertj.core.api.Assertions.assertThat;

class PdfA4ValidatorTest {

	private static final String DOKUMENT_OBJEKT_REFERANSE = "test-dok-ref-123";
	private static final String BESTILLINGS_ID = "test-bestilling-456";
	private static final Logger VALIDATOR_LOG = (Logger) LoggerFactory.getLogger(PdfA4Validator.class);

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void settOppLogCapture() {
		logAppender = new ListAppender<>();
		logAppender.start();
		VALIDATOR_LOG.addAppender(logAppender);
	}

	@AfterEach
	void ryddOppLogCapture() {
		VALIDATOR_LOG.detachAppender(logAppender);
	}

	@ParameterizedTest
	@ValueSource(strings = {"__files/pdf/A4_portrett.pdf", "__files/pdf/A4.pdf", "__files/pdf/A5.pdf"})
	void skalIkkeLoggeErrorForGyldigStorrelse(String ressurssti) throws IOException {
		loggHvisDetFinnesPagesSomErStoerreEnnA4(lagDokumentListe(ressurssti), BESTILLINGS_ID);

		assertThat(logAppender.list).noneMatch(e -> e.getLevel() == Level.ERROR);
	}

	@Test
	void skalLoggeErrorForA3() throws IOException {
		loggHvisDetFinnesPagesSomErStoerreEnnA4(lagDokumentListe("__files/pdf/A3.pdf"), BESTILLINGS_ID);

		assertThat(logAppender.list)
				.anyMatch(e -> e.getLevel() == Level.ERROR
						&& e.getFormattedMessage().contains(BESTILLINGS_ID)
						&& e.getFormattedMessage().contains(DOKUMENT_OBJEKT_REFERANSE));
	}

	@Test
	void skalLoggeWarnForUgyldigePdfBytes() {
		DokdistDokument ugyldigDokument = DokdistDokument.builder()
				.pdf("not a pdf".getBytes())
				.dokumentObjektReferanse(DOKUMENT_OBJEKT_REFERANSE)
				.build();

		loggHvisDetFinnesPagesSomErStoerreEnnA4(List.of(ugyldigDokument), BESTILLINGS_ID);

		assertThat(logAppender.list)
				.anyMatch(e -> e.getLevel() == Level.WARN
						&& e.getFormattedMessage().contains(BESTILLINGS_ID)
						&& e.getFormattedMessage().contains(DOKUMENT_OBJEKT_REFERANSE));
	}

	private List<DokdistDokument> lagDokumentListe(String ressurssti) throws IOException {
		byte[] pdf = Objects.requireNonNull(
				getClass().getClassLoader().getResourceAsStream(ressurssti),
				"Fant ikke testfil: " + ressurssti
		).readAllBytes();
		return List.of(DokdistDokument.builder()
				.pdf(pdf)
				.dokumentObjektReferanse(DOKUMENT_OBJEKT_REFERANSE)
				.build());
	}
}
