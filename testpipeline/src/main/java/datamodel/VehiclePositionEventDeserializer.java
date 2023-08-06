package datamodel;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class VehiclePositionEventDeserializer implements DeserializationSchema<VehiclePositionEvent>{
    private static final long serialVersionUID = 1L;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public VehiclePositionEvent deserialize(byte[] message) throws IOException {
		return objectMapper.readValue(message, VehiclePositionEvent.class);
	}

	@Override
	public boolean isEndOfStream(VehiclePositionEvent nextElement) {
		return false;
	}

	@Override
	public TypeInformation<VehiclePositionEvent> getProducedType() {
		return TypeInformation.of(VehiclePositionEvent.class);
	}
}
