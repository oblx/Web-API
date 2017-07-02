package valandur.webapi.json.serializer.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.spongepowered.api.data.manipulator.mutable.entity.TradeOfferData;
import org.spongepowered.api.item.merchant.TradeOffer;
import valandur.webapi.api.json.WebAPIBaseSerializer;

import java.io.IOException;

public class TradeOfferDataSerializer extends WebAPIBaseSerializer<TradeOfferData> {
    @Override
    public void serialize(TradeOfferData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (TradeOffer offer : value.asList()) {
            writeValue(provider, offer);
        }
        gen.writeEndArray();
    }
}
