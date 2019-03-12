package no.nav.dokdistsentralprint.consumer.regoppslag;

import no.nav.dokdistsentralprint.consumer.regoppslag.to.HentAdresseRequestTo;
import no.nav.dokdistsentralprint.exception.technical.RegoppslagSecurityException;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface Regoppslag {

	AdresseTo treg002HentAdresse(HentAdresseRequestTo request) throws RegoppslagSecurityException;
}