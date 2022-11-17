package no.nav.dokdistsentralprint.qdist009.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Adresse {

	String adresselinje1;
	String adresselinje2;
	String adresselinje3;
	String postnummer;
	String poststed;
	String landkode;
}
