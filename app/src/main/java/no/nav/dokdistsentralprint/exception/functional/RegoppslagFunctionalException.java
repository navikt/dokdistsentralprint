package no.nav.dokdistsentralprint.exception.functional;


/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class RegoppslagFunctionalException extends AbstractDokdistsentralprintFunctionalException {
	
	
	private final String serviceName;
	private final String detailedMessage;
	
	public RegoppslagFunctionalException(String message, String detailedMessage, String serviceName) {
		super(message);
		this.serviceName = serviceName;
		this.detailedMessage = detailedMessage;
	}

	public RegoppslagFunctionalException(String message, String shortMessage, String detailedMessage, String serviceName) {
		super(message, shortMessage);
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
