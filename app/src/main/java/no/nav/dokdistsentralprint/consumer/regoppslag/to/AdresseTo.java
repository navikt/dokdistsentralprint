package no.nav.dokdistsentralprint.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdresseTo {

	String adresselinje1;
	String adresselinje2;
	String adresselinje3;
	String postnummer;
	String poststed;
	String landkode;
}
