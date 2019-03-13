package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;


@Builder
@AllArgsConstructor
@Value
public class HentAdresseRequestTo {
	private final String identifikator;
	private final String type;
}
