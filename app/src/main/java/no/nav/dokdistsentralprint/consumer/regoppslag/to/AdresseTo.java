package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@Value
public class AdresseTo {

	private final String adresselinje1;
	private final String adresselinje2;
	private final String adresselinje3;
	private final String postnummer;
	private final String poststed;
	private final String landkode;
}
