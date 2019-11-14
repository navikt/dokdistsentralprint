package no.nav.dokdistsentralprint.qdist009.util.cdata;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Olav Røstvold Thorsen, Visma Consulting.
 */
public class CDataAdapter extends XmlAdapter<String, String> {

	public static final String CDATA_PREFIX = "<![CDATA[";

	@Override
	public String marshal(String v) throws Exception {
		return CDATA_PREFIX + v;
	}

	@Override
	public String unmarshal(String v) throws Exception {
		return v;
	}
}
