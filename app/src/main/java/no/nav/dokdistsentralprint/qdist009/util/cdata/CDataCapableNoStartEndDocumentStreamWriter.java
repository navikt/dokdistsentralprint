package no.nav.dokdistsentralprint.qdist009.util.cdata;

import org.codehaus.stax2.ri.evt.CharactersEventImpl;
import org.springframework.batch.item.xml.stax.NoStartEndDocumentStreamWriter;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class CDataCapableNoStartEndDocumentStreamWriter extends NoStartEndDocumentStreamWriter {

	public CDataCapableNoStartEndDocumentStreamWriter(XMLEventWriter wrappedEventWriter) {
		super(wrappedEventWriter);
	}

	@Override
	public void add(XMLEvent event) throws XMLStreamException {
		if ((!event.isStartDocument()) && (!event.isEndDocument())) {

			if (event.isCharacters() && event.asCharacters().getData().startsWith(CDataAdapter.CDATA_PREFIX)) {
				XMLEvent cdataEvent = createCDataEventFromCharacterEvent(event);
				wrappedEventWriter.add(cdataEvent);
			} else {
				wrappedEventWriter.add(event);
			}

		}
	}

	private XMLEvent createCDataEventFromCharacterEvent(XMLEvent event) {
		return new CharactersEventImpl(event.getLocation(),
				event.asCharacters().getData().substring(CDataAdapter.CDATA_PREFIX.length()), true);
	}

}

