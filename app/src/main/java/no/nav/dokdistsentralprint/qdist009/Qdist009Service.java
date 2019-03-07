package no.nav.dokdistsentralprint.qdist009;

import no.nav.dokdistsentralprint.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistsentralprint.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistsentralprint.kodeverk.TilknyttetSomCode;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist009Service {

	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final AdministrerForsendelse administrerForsendelse;

	@Inject
	public Qdist009Service(DokumentkatalogAdmin dokumentkatalogAdmin,
						   AdministrerForsendelse administrerForsendelse) {
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public File distribuerForsendelseService(DistribuerForsendelseTilSentralPrintTo distribuerForsendelseTilSentralPrintTo, Exchange exchange) {

		return new File("");
	}

	private String getDokumenttypeIdHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.HOVEDDOKUMENT))
				.map(DistribuerForsendelseTo.DokumentInformasjonTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}


}
