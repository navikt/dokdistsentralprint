package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HentAdresseRequestTo {
	String identifikator;
	String type;
}
