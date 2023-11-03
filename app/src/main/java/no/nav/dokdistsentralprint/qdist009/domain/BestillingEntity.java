package no.nav.dokdistsentralprint.qdist009.domain;

import lombok.Builder;
import lombok.Value;

import static java.lang.String.format;

@Value
@Builder
public class BestillingEntity {

	String fileName;
	byte[] entity;

	@Override
	public String toString() {
		return format("BestillingEntity{filnavn=%s, entity.length=%s}", this.fileName, this.entity == null ? null : this.entity.length);
	}
}
