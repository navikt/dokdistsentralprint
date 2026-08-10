package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentMottakerOgAdresseResponseTo {

	AdresseTo adresse;
}
