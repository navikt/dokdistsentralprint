package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.dokdistsentralprint.consumer.regoppslag.AdresseTo;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HentMottakerOgAdresseResponseTo {
	String identifikator;
	String navn;
	AdresseTo adresse;
}
