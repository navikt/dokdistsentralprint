package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Value;


@Data
@Builder
@AllArgsConstructor
@Value
public class HentAdresseRequestTo {
	private final String identifikator;
	private final String type;
}
