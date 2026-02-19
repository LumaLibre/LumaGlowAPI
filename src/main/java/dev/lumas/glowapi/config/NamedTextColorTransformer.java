package dev.lumas.glowapi.config;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;

public class NamedTextColorTransformer extends BidirectionalTransformer<String, NamedTextColor> {
    @Override
    public GenericsPair<String, NamedTextColor> getPair() {
        return this.genericsPair(String.class, NamedTextColor.class);
    }

    @Override
    public NamedTextColor leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        NamedTextColor fromString = NamedTextColor.NAMES.value(data.toLowerCase());
        if (fromString == null) {
            throw new IllegalArgumentException("Invalid color: " + data);
        }
        return fromString;
    }

    @Override
    public String rightToLeft(@NonNull NamedTextColor data, @NonNull SerdesContext serdesContext) {
        return data.toString().toUpperCase();
    }
}
