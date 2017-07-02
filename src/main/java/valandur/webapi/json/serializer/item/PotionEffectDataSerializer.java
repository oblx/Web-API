package valandur.webapi.json.serializer.item;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.effect.potion.PotionEffect;
import valandur.webapi.api.json.WebAPIBaseSerializer;

import java.io.IOException;

public class PotionEffectDataSerializer extends WebAPIBaseSerializer<PotionEffectData> {
    @Override
    public void serialize(PotionEffectData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (PotionEffect effect : value.asList()) {
            writeValue(provider, effect);
        }
        gen.writeEndArray();
    }
}
