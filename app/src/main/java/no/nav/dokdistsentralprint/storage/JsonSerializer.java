package no.nav.dokdistsentralprint.storage;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import static tools.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS;

public class JsonSerializer {

	private static final JsonMapper jsonMapper = JsonMapper.builder()
			.enable(ALLOW_JAVA_COMMENTS)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	private static final ObjectWriter writer = jsonMapper.writer();

	public static String serialize(Object object) {
		try {
			return writer.writeValueAsString(object);
		} catch (JacksonException e) {
			throw new IllegalStateException(e);
		}
	}

	public static <T> T deserialize(String jsonPayload, Class<T> tClass) {
		try {
			if (jsonPayload == null) {
				throw new IllegalStateException("jsonPayload er null.");
			}
			return jsonMapper.readValue(jsonPayload, tClass);
		} catch (JacksonException e) {
			throw new IllegalStateException(e);
		}
	}
}
