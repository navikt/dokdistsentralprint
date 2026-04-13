package no.nav.dokdistsentralprint.qdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.storage.DokdistDokument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.util.List;

import static org.apache.pdfbox.pdmodel.common.PDRectangle.A4;

@Slf4j
public class PdfA4Validator {

	private static final float A4_TOLERANCE = 1.0f;

	public static void loggDokumenterSomErStoerreEnnA4(List<DokdistDokument> dokumentListe, String bestillingsId) {
		dokumentListe.forEach(DokdistDokument -> loggDokumenterSomErStoerreEnnA4(DokdistDokument, bestillingsId));

	}

	private static void loggDokumenterSomErStoerreEnnA4(DokdistDokument dokdistDokument, String bestillingsId) {
		try (PDDocument pdDocument = Loader.loadPDF(dokdistDokument.getPdf())) {
			for (PDPage page : pdDocument.getPages()) {
				PDRectangle mediaBox = page.getMediaBox();
				if (dokumentErStoerreEnnA4(mediaBox)) {
					log.error("Dokumentet tilhørende bestillingsId:{} er ikke A4. Dokumentobjektreferanse: {}", bestillingsId, dokdistDokument.getDokumentObjektReferanse());
				}
			}
		} catch (IOException e) {
			log.warn("Klarte ikke å validere om dokument er A4. BestillingsId:{}, Dokumentobjektreferanse: {}. Exception: {}", bestillingsId, dokdistDokument.getDokumentObjektReferanse(), e.getMessage());
		}
	}

	private static boolean dokumentErStoerreEnnA4(PDRectangle mediaBox) {
		float shortSide = Math.min(mediaBox.getWidth(), mediaBox.getHeight());
		float longSide = Math.max(mediaBox.getWidth(), mediaBox.getHeight());
		return shortSide > A4.getWidth() + A4_TOLERANCE
				|| longSide > A4.getHeight() + A4_TOLERANCE;
	}
}
