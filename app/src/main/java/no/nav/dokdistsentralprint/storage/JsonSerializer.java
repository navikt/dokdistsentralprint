package no.nav.dokdistsentralprint.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class JsonSerializer {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.configure(ALLOW_COMMENTS, true);
		objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	private static final ObjectWriter writer = objectMapper.writer();

	public static String serialize(Object object) {
		try {
			return writer.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static <T> T deserialize(String jsonPayload, Class<T> tClass) {
		try {
			if (jsonPayload == null) {
				throw new IllegalStateException("jsonPayload er null.");
			}
			return objectMapper.readValue(jsonPayload, tClass);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
