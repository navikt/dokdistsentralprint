package no.nav.dokdistsentralprint.qdist009;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static no.nav.dokdistsentralprint.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistsentralprint.qdist009.Qdist009Route.PROPERTY_FORSENDELSE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class IdsProcessorTest {
    private static final Object FORSENDELSE_ID = "33333";
    public static final String CALL_ID_VALUE = "mycallid";
    private final CamelContext camelContext = new DefaultCamelContext();
    private final IdsProcessor idsProcessor = new IdsProcessor();

    @AfterEach
    void afterEach() {
        MDC.clear();
    }

    @Test
    void shouldSetForsendelseIdProperty() throws IOException {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(defaultQdist009Message());
        idsProcessor.process(exchange);
        assertThat(exchange.getProperty(PROPERTY_FORSENDELSE_ID)).isEqualTo(FORSENDELSE_ID);
    }

    @Test
    void shouldPutCallIdOnMdcWhenIncludedInHeader() throws IOException {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(CALL_ID, CALL_ID_VALUE);
        exchange.getIn().setBody(defaultQdist009Message());
        idsProcessor.process(exchange);
        assertThat(MDC.get(CALL_ID)).isNotNull();
        assertThat(MDC.get(CALL_ID)).isEqualTo(CALL_ID_VALUE);
    }

    @Test
    void shouldPutExchangeIdAsCallidMdcWhenNotIncludedInHeader() throws IOException {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(defaultQdist009Message());
        idsProcessor.process(exchange);
        assertThat(MDC.get(CALL_ID)).isNotNull();
        assertThat(MDC.get(CALL_ID)).isEqualTo(exchange.getExchangeId());
    }

    private Object defaultQdist009Message() throws IOException {
        return IOUtils.toString(new ClassPathResource("qdist009/qdist009-happy.xml").getInputStream());
    }
}