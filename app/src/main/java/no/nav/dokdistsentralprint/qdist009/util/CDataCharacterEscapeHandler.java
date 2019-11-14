package no.nav.dokdistsentralprint.qdist009.util;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class CDataCharacterEscapeHandler implements CharacterEscapeHandler {

	private static final char[] cdataPrefix = "<![CDATA[".toCharArray();
	private static final char[] cDataSuffix = "]]>".toCharArray();
	private static final char[] chars = {'\\', '\\', 'r'};

	@Override
	public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
		String s = new String(ch).substring(start, start + length).replaceAll("\r", new String(chars));
		out.write(s);
	}
}
