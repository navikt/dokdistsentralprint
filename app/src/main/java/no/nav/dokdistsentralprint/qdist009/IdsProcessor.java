package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.exception.functional.ForsendelseManglerForsendelseIdFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.slf4j.MDC;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_FORSENDELSE_ID;

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
        final String callIdHeader = exchange.getIn().getHeader(CALL_ID, String.class);
        if (callIdHeader == null || callIdHeader.trim().isEmpty()) {
            final String exchangeId = exchange.getExchangeId();
            MDC.put(CALL_ID, exchangeId);
        } else {
            MDC.put(CALL_ID, callIdHeader);
        }
    }

    private void setForsendelseIdAsProperty(Exchange exchange) {
        String forsendelseId = XPathBuilder.xpath("//forsendelseId/text()").evaluate(exchange, String.class);
        if (forsendelseId.trim().isEmpty()) {
            throw new ForsendelseManglerForsendelseIdFunctionalException("qdist009 har mottatt forsendelse med uten påkrevd forsendelseId");
        }
        exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
    }
}
