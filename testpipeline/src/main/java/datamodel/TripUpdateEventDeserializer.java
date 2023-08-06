package datamodel;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class TripUpdateEventDeserializer implements DeserializationSchema<TripUpdateEvent>{
    private static final long serialVersionUID = 1L;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public TripUpdateEvent deserialize(byte[] message) throws IOException {
		return objectMapper.readValue(message, TripUpdateEvent.class);
	}

	@Override
	public boolean isEndOfStream(TripUpdateEvent nextElement) {
		return false;
	}

	@Override
	public TypeInformation<TripUpdateEvent> getProducedType() {
		return TypeInformation.of(TripUpdateEvent.class);
	}
}
