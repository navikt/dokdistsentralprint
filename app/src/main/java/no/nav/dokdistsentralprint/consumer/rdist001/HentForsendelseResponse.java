package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HentForsendelseResponse {
	long forsendelseId;
	String bestillingsId;
	String originalBestillingsId;
	String forsendelseStatus;
	String modus;
	String tema;
	Mottaker mottaker;
	Postadresse postadresse;
	List<Dokument> dokumenter;
	private ArkivInformasjon arkivInformasjon;

	@Data
	@Builder
	public static class ArkivInformasjon {
		ArkivSystemCode arkivSystem;
		String arkivId;
	}

	@Data
	@Builder
	public static class Mottaker {
		String mottakerId;
		String mottakerNavn;
		String mottakerType;
	}

	@Data
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
		String tilknyttetSom;
		String dokumentObjektReferanse;
		String dokumenttypeId;
	}

	public enum ArkivSystemCode {
		JOARK, MIDL_BREVLAGER, INGEN
	}
}

