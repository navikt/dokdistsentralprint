package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Data
@Builder
public class HentForsendelseResponse {
	private final String bestillingsId;
	private final String originalBestillingsId;
	private final String forsendelseStatus;
	private final String modus;
	private final String tema;
	private final Mottaker mottaker;
	private final Postadresse postadresse;
	private final List<Dokument> dokumenter;


	@Data
	@Builder
	public static class Mottaker {
		private final String mottakerId;
		private final String mottakerNavn;
		private final String mottakerType;
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

	@Data
	@Builder
	public static class Dokument {
		private final String tilknyttetSom;
		private final String dokumentObjektReferanse;
		private final String dokumenttypeId;
	}
}

