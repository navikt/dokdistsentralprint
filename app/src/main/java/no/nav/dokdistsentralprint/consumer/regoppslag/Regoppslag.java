package no.nav.dokdistsentralprint.consumer.regoppslag;

import no.nav.dokdistsentralprint.consumer.regoppslag.to.AdresseTo;
import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;

public interface Regoppslag {

	AdresseTo treg002HentAdresse(HentAdresseRequestTo request);
}