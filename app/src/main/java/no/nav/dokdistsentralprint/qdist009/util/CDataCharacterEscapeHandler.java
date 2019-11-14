package no.nav.dokdistsentralprint.qdist009.util;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import com.sun.xml.bind.marshaller.MinimumEscapeHandler;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class CDataCharacterEscapeHandler implements CharacterEscapeHandler {

	private static final char[] cdataPrefix = "<![CDATA[".toCharArray();
	private static final char[] cDataSuffix = "]]>".toCharArray();

	@Override
	public void escape(char[] ch, int start, int length, boolean isAttVal, Writer out) throws IOException {
		boolean isCData = length > cdataPrefix.length + cDataSuffix.length;
		if(isCData){
			for(int i = 0, j = start; i < cdataPrefix.length; ++i , ++j) {
				if (cdataPrefix[i] != ch[j]) {
					isCData = false;
					break;
				}
			}
		}if(isCData){
			for(int i = cDataSuffix.length -1, j = start + length -1; i >= 0; --i, --j) {
				if (cDataSuffix[i] != ch[j]) {
					isCData = false;
					break;
				}
			}

		}if(isCData){
			out.write(ch, start,length);
		} else {
			MinimumEscapeHandler.theInstance.escape(ch, start, length, isAttVal, out);
		}
	}
}
