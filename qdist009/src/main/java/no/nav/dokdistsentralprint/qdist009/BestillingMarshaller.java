package no.nav.dokdistsentralprint.qdist009;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import no.nav.dokdistsentralprint.exception.technical.KunneIkkeMarshalleBestillingTechnicalException;
import no.nav.dokdistsentralprint.printoppdrag.Bestilling;
import no.nav.dokdistsentralprint.qdist009.util.CDataCharacterEscapeHandler;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

import static jakarta.xml.bind.Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION;

@Component
class BestillingMarshaller {
	// Implementasjoner av JAXBContext skal være trådsikre
	private final JAXBContext jaxbContext;

	public BestillingMarshaller() {
		try {
			this.jaxbContext = JAXBContext.newInstance(Bestilling.class);
		} catch (JAXBException e) {
			throw new IllegalStateException("Kunne ikke opprette JAXBContext", e);
		}
	}

	String marshalBestillingToXmlString(Bestilling bestilling) {
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "printoppdrag-2_2.xsd");
			marshaller.setProperty("org.glassfish.jaxb.characterEscapeHandler", new CDataCharacterEscapeHandler());
			StringWriter sw = new StringWriter();
			marshaller.marshal(bestilling, sw);
			return sw.toString();
		} catch (JAXBException | IllegalArgumentException e) {
			throw new KunneIkkeMarshalleBestillingTechnicalException("Kunne ikke marshalle bestilling til xmlString", e);
		}
	}
}
