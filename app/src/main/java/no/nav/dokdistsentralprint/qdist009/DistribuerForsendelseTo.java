package no.nav.dokdistsentralprint.qdist009;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import no.nav.dokdistsentralprint.kodeverk.ArkivSystemCode;
import no.nav.dokdistsentralprint.kodeverk.MottakerTypeCode;
import no.nav.dokdistsentralprint.kodeverk.TemaCode;
import no.nav.dokdistsentralprint.kodeverk.TilknyttetSomCode;

import java.util.List;

/**
 * @author Sigurd Midttun, Visma Consulting AS
 */

@Value
@Builder
public class DistribuerForsendelseTo {

	private final DistribusjonbestillingTo distribusjonbestilling;

	@Value
	@Builder
	public static class DistribusjonbestillingTo {
		private final String bestillingsId;
		private final String batchId;
		private final String bestillendeFagsystem;
		private final TemaCode tema;
		private final String forsendelseTittel;
		private final ArkivInformasjonTo arkivInformasjon;
		private final MottakerTo mottaker;
		private final AdresseTo adresse;
		private final String dokumentProdApp;
		private final List<DokumentInformasjonTo> dokumenter;
	}

	@Value
	@Builder
	public static class MottakerTo {
		private final String identifikator;
		private final String navn;
		private final boolean identifikatorAktoerId;
		private final MottakerTypeCode mottakerType;

		public boolean isSamhandler() {
			return this.getMottakerType().equals(MottakerTypeCode.SAMHANDLER_HPR);
		}

	}

	@Getter
	@AllArgsConstructor
	public abstract static class AdresseTo {
		private final String adresselinje1;
		private final String adresselinje2;
		private final String adresselinje3;
		private final String land;
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	public static class NorskPostadresseTo extends AdresseTo {
		private final String postnummer;
		private final String poststed;

		@Builder
		public NorskPostadresseTo(String adresselinje1, String adresselinje2, String adresselinje3, String land, String postnummer, String poststed) {
			super(adresselinje1, adresselinje2, adresselinje3, land);
			this.postnummer = postnummer;
			this.poststed = poststed;
		}
	}

	@EqualsAndHashCode(callSuper = true)
	@Value
	public static class UtenlandskPostadresseTo extends AdresseTo {
		@Builder
		public UtenlandskPostadresseTo(String adresselinje1, String adresselinje2, String adresselinje3, String land) {
			super(adresselinje1, adresselinje2, adresselinje3, land);
		}
	}

	@Value
	@Builder
	public static class ArkivInformasjonTo {
		private final ArkivSystemCode arkivSystem;
		private final String arkivId;
	}

	@Value
	@Builder
	public static class DokumentInformasjonTo {
		private final String dokumenttypeId;
		private final String dokumentObjektReferanse;
		private final TilknyttetSomCode tilknyttetSom;
		private final String arkivDokumentInfoId;
		private final int rekkefolge;
	}

}
