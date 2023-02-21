package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Data
@Builder
public class HentForsendelseResponseTo {
	private final String bestillingsId;
	private final String originalBestillingsId;
	private final String forsendelseStatus;
	private final String modus;
	private final String tema;
	private final MottakerTo mottaker;
	private final PostadresseTo postadresse;
	private final List<DokumentTo> dokumenter;


	@Data
	@Builder
	public static class MottakerTo {
		private final String mottakerId;
		private final String mottakerNavn;
		private final String mottakerType;
	}

	@Value
	@Builder
	public static class PostadresseTo {
		String adresselinje1;
		String adresselinje2;
		String adresselinje3;
		String postnummer;
		String poststed;
		String landkode;
	}

	@Data
	@Builder
	public static class DokumentTo {
		private final String tilknyttetSom;
		private final String dokumentObjektReferanse;
		private final String dokumenttypeId;
	}
}

