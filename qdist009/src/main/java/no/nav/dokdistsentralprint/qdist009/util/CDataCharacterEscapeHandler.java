package no.nav.dokdistsentralprint.qdist009.util;

import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler;
import org.glassfish.jaxb.core.marshaller.MinimumEscapeHandler;

import java.io.IOException;
import java.io.Writer;

public class CDataCharacterEscapeHandler implements CharacterEscapeHandler {

	private static final char[] cdataPrefix = "<![CDATA[".toCharArray();
	private static final char[] cDataSuffix = "]]>".toCharArray();
	private static final char[] chars = {'\\', '\\', 'r'};

	@Override
	public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
		boolean isCData = length > cdataPrefix.length + cDataSuffix.length;
		if (isCData) {
			for (int i = 0, j = start; i < cdataPrefix.length; ++i, ++j) {
				if (cdataPrefix[i] != ch[j]) {
					isCData = false;
					break;
				}
			}
		}
		if (isCData) {
			for (int i = cDataSuffix.length - 1, j = start + length - 1; i >= 0; --i, --j) {
				if (cDataSuffix[i] != ch[j]) {
					isCData = false;
					break;
				}
			}

		}
		if (isCData) {
			String s = new String(ch).substring(start, start + length).replaceAll("\r", new String(chars));
			out.write(s);
		} else {
			MinimumEscapeHandler.theInstance.escape(ch, start, length, isAttVal, out);
		}
	}
}
