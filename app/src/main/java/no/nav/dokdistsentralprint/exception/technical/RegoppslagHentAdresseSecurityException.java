package no.nav.dokdistsentralprint.exception.technical;


/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class RegoppslagHentAdresseSecurityException extends AbstractDokdistsentralprintTechnicalException {
	
	private final String serviceName;
	private final String detailedMessage;
	
	public RegoppslagHentAdresseSecurityException(String message, String detailedMessage, String serviceName) {
		super(message);
		this.serviceName = serviceName;
		this.detailedMessage = detailedMessage;
		
	}
	
	public String getDetailedMessage() {
		return this.detailedMessage;
	}
	
	public String getServiceName() {
		if ("TREG001".equalsIgnoreCase(serviceName)) {
			return serviceName + " komplettering av brevdata";
		} else if ("TREG002".equalsIgnoreCase(serviceName)) {
			return serviceName + " henting av mottaker og adresse";
		}
		return serviceName;
	}
}
