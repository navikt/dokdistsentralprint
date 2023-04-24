package no.nav.dokdistsentralprint.consumer.rdist001;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OppdaterForsendelseRequest {
	private Long forsendelseId;
	private String forsendelseStatus;
	private String konversasjonId;
	private String varselStatus;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
}
