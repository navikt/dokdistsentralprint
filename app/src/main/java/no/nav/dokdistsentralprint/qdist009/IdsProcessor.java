package no.nav.dokdistsentralprint.qdist009;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_FORSENDELSE_ID;

import no.nav.dokdistsentralprint.exception.functional.ForsendelseManglerForsendelseIdFunctionalException;
import no.nav.dokdistsentralprint.exception.functional.ForsendelseManglerPaakrevdHeaderFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.MDC;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class IdsProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		setBestillingsIdAsPropertyAndAddCallIdToMdc(exchange);
		setForsendelseIdAsProperty(exchange);
	}

	private void setBestillingsIdAsPropertyAndAddCallIdToMdc(Exchange exchange) {
		final String callId = exchange.getIn().getHeader(CALL_ID, String.class);
		if (callId == null) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException(
					"qdist009 har mottatt forsendelse uten påkrevd header callId");
		} else if (callId.trim().isEmpty()) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException(
					"qdist009 har mottatt forsendelse med tom header callId");
		}
		MDC.put(CALL_ID, callId);
	}

	private void setForsendelseIdAsProperty(Exchange exchange) {
		String forsendelseId = XPathBuilder.xpath("//forsendelseId/text()") .evaluate(exchange, String.class);
		if (forsendelseId.trim().isEmpty()) {
			throw new ForsendelseManglerForsendelseIdFunctionalException("qdist009 har mottatt forsendelse med uten påkrevd forsendelseId");
		}
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
	}
}
