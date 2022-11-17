package no.nav.dokdistsentralprint.qdist009.domain;

import static java.lang.String.format;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BestillingEntity {
	String fileName;
	byte[] entity;

	public String toString() {
		return format("BestillingEntity{filnavn=%s, entity.length=%s}", this.fileName, this.entity == null ? null : this.entity.length);
	}
}
