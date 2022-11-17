package no.nav.dokdistsentralprint.qdist009.cdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import no.nav.dokdistsentralprint.qdist009.util.CDataCharacterEscapeHandler;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.StringWriter;

class CDataEscapeHandlerTest {

	CDataCharacterEscapeHandler cDataCharacterEscapeHandler = new CDataCharacterEscapeHandler();
	StringWriter sw = new StringWriter();

	@Test
	void shouldNotEscapeCharactersInCData() throws IOException {

		String cdata= "<![CDATA[mottakerNavn\\rlinje1\\rlinje2\\rlinje3\\r3020 Drammen\\rTR]]>";
		char[] test = cdata.toCharArray();
		cDataCharacterEscapeHandler.escape(test, 0, test.length, true, sw);
		assertEquals(cdata, sw.toString());
	}

	@Test
	void shouldEscapeCharacters() throws IOException {
		String xml= "&,<>,\",\r";
		String xmlEscaped = "&amp;,&lt;&gt;,&quot;,&#13;";
		char[] test = xml.toCharArray();
		cDataCharacterEscapeHandler.escape(test, 0, test.length, true, sw);
		assertEquals(xmlEscaped, sw.toString());
	}
}
