package no.nav.dokdistsentralprint.sdist009;

import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse;
import no.nav.dokdistsentralprint.consumer.rdist001.HentForsendelseResponse.ArkivInformasjon;
import no.nav.dokdistsentralprint.exception.functional.OpprettReturpostoppgaveException;
import no.nav.dokdistsentralprint.exception.technical.DokdistsentralprintTechnicalException;
import no.nav.opprettoppgave.tjenestespesifikasjon.ObjectFactory;
import no.nav.opprettoppgave.tjenestespesifikasjon.OpprettOppgave;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;

import java.io.StringWriter;

import static java.lang.String.format;
import static no.nav.dokdistsentralprint.consumer.rdist001.ArkivSystemCode.JOARK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
public class ReturpostOppgaveService {

	public static final String BEHANDLE_RETURPOST = "BEHANDLE RETURPOST";

	private final Queue qopp001;
	private final JAXBContext jaxbContext;
	private final ProducerTemplate producerTemplate;

	public ReturpostOppgaveService(ProducerTemplate producerTemplate, Queue qopp001) {
		this.qopp001 = qopp001;
		this.producerTemplate = producerTemplate;
		try {
			jaxbContext = JAXBContext.newInstance(OpprettOppgave.class);
		} catch (JAXBException e) {
			throw new DokdistsentralprintTechnicalException("Feil ved opprettelse av JAXBContext for OpprettOppgave", e);
		}
	}

	public void opprettReturpostoppgave(HentForsendelseResponse forsendelse) {
		ArkivInformasjon arkivinformasjon = forsendelse.getArkivInformasjon();
		if (!erArkivinformasjonGyldig(arkivinformasjon)) {
			loggUgyldigArkivinformasjon(arkivinformasjon, forsendelse.getBestillingsId());
			return;
		}

		OpprettOppgave opprettOppgave = mapOpprettOppgave(arkivinformasjon);

		try {
			producerTemplate.sendBody("jms:" + qopp001.getQueueName(), marshallOppgave(opprettOppgave));
		} catch (JMSException e) {
			throw new OpprettReturpostoppgaveException(format("Feil ved opprettelse av returpostoppgave med bestillingsId=%s og arkivId=%s", forsendelse.getBestillingsId(), arkivinformasjon.getArkivId()), e);
		}
	}

	private OpprettOppgave mapOpprettOppgave(ArkivInformasjon arkivInformasjon) {
		OpprettOppgave opprettOppgave = new OpprettOppgave();
		opprettOppgave.setArkivKode(arkivInformasjon.getArkivId());
		opprettOppgave.setArkivSystem(arkivInformasjon.getArkivSystem().name());
		opprettOppgave.setOppgaveType(BEHANDLE_RETURPOST);
		return opprettOppgave;
	}

	private String marshallOppgave(OpprettOppgave oppgave) {
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			ObjectFactory factory = new ObjectFactory();
			JAXBElement<OpprettOppgave> root = factory.createOpprettOppgave(oppgave);

			StringWriter sw = new StringWriter();
			marshaller.marshal(root, sw);

			return sw.toString();
		} catch (JAXBException e) {
			log.error("Kunne ikke opprette OpprettOppgave-XML: {}", e.getMessage(), e);
			throw new DokdistsentralprintTechnicalException("Kunne ikke opprette OpprettOppgave-XML", e);
		}
	}

	private void loggUgyldigArkivinformasjon(ArkivInformasjon arkivinformasjon, String bestillingsId) {
		if (arkivinformasjon == null) {
			log.warn("Sdist009 har mottatt returpost der arkivinformasjon=null for bestillingsId={}. " +
					"Oppgave for manuell oppfølging opprettes ikke i Gosys. Avslutter behandling av kvittering og går til neste.", bestillingsId);
		} else {
			log.warn("Sdist009 har mottatt returpost med arkivSystem={} og arkivId={} for bestillingsId={}. " +
							"Oppgave for manuell oppfølging opprettes ikke i Gosys. Avslutter behandling av kvittering og går til neste.",
					arkivinformasjon.getArkivSystem(), arkivinformasjon.getArkivId(), bestillingsId);
		}
	}

	private boolean erArkivinformasjonGyldig(ArkivInformasjon arkivinformasjon) {
		return arkivinformasjon != null
				&& isNotBlank(arkivinformasjon.getArkivId())
				&& JOARK.equals(arkivinformasjon.getArkivSystem());
	}

}