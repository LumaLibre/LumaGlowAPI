package dev.lumas.glowapi.config;

import dev.lumas.glowapi.model.GlowStyle;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;

public class GlowStyleTransformer extends BidirectionalTransformer<String, GlowStyle> {

    @Override
    public GenericsPair<String, GlowStyle> getPair() {
        return this.genericsPair(String.class, GlowStyle.class);
    }

    @Override
    public GlowStyle leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
        GlowStyle style = GlowStyle.parse(data);
        if (style == null) {
            throw new IllegalArgumentException("Invalid glow style: '" + data
                    + "' (expected a color name, a preset, #rrggbb, rainbow[:speed], gradient:#rrggbb[:span] or pulse:#rrggbb[:speed])");
        }
        return style;
    }

    @Override
    public String rightToLeft(@NonNull GlowStyle data, @NonNull SerdesContext serdesContext) {
        return data.serialize();
    }
}
