package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AdresseTo {
	
	String adresselinje1;
	String adresselinje2;
	String adresselinje3;
	String postnummer;
	String poststed;
	String landkode;
}
