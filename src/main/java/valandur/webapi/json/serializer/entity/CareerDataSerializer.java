package valandur.webapi.json.serializer.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.spongepowered.api.data.manipulator.mutable.entity.CareerData;
import valandur.webapi.api.json.WebAPIBaseSerializer;

import java.io.IOException;

public class CareerDataSerializer extends WebAPIBaseSerializer<CareerData> {
    @Override
    public void serialize(CareerData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        writeValue(provider, value.type().get());
    }
}
