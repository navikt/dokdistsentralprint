package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class HentForsendelseResponse {
	String bestillingsId;
	String originalBestillingsId;
	String forsendelseStatus;
	String modus;
	String tema;
	Mottaker mottaker;
	Postadresse postadresse;
	List<Dokument> dokumenter;

	@Value
	@Builder
	public static class Mottaker {
		String mottakerId;
		String mottakerNavn;
		String mottakerType;
	}

	@Value
	@Builder
	public static class Postadresse {
		String adresselinje1;
		String adresselinje2;
		String adresselinje3;
		String postnummer;
		String poststed;
		String landkode;
	}

	@Value
	@Builder
	public static class Dokument {
		String tilknyttetSom;
		String dokumentObjektReferanse;
		String dokumenttypeId;
	}
}

