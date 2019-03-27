package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentAdresseRequestTo {

	private final String identifikator;
	private final String type;

}
