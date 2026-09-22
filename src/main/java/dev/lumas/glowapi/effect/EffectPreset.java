package dev.lumas.glowapi.effect;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum EffectPreset {

    RAINBOW("rainbow", GlowEffect.rainbow(), 0xFF55FF, "rainbow"),
    RAINBOW_SLOW("rainbow_slow", GlowEffect.rainbow(3), 0xFF55FF, "slow rainbow"),
    RAINBOW_FAST("rainbow_fast", GlowEffect.rainbow(15), 0xFF55FF, "fast rainbow"),
    FIRE("fire", GlowEffect.gradient(0xFF0000, 3), 0xFF4400, "red to yellow"),
    SUNSET("sunset", GlowEffect.gradient(0xFF0080, 3), 0xFF6688, "pink to orange"),
    FOREST("forest", GlowEffect.gradient(0xAAFF00, 4), 0x66CC33, "yellow-green to teal"),
    ACID("acid", GlowEffect.gradient(0xEAFF00, 2), 0xCCFF00, "yellow to lime"),
    OCEAN("ocean", GlowEffect.gradient(0x00FFD5, 4), 0x00CCFF, "cyan to violet"),
    ICE("ice", GlowEffect.gradient(0x00EAFF, 2), 0x66DDFF, "cyan to blue"),
    GALAXY("galaxy", GlowEffect.gradient(0x5500FF, 5), 0x9944FF, "violet to magenta"),
    CANDY("candy", GlowEffect.gradient(0xFF00FF, 6), 0xFF44CC, "magenta through red to yellow"),
    PULSE_RED("pulse_red", GlowEffect.pulse(0xFF0000, 12), 0xFF3333, "fast red pulse"),
    PULSE_AQUA("pulse_aqua", GlowEffect.pulse(0x00FFFF, 8), 0x33FFFF, "aqua pulse"),
    PULSE_PINK("pulse_pink", GlowEffect.pulse(0xFF00FF, 8), 0xFF33FF, "pink pulse"),
    PULSE_YELLOW("pulse_yellow", GlowEffect.pulse(0xFFEA00, 4), 0xFFEE33, "slow yellow pulse"),

    MISTRAL_SLOW("mistral_slow", List.of(0xFFF2BE, 0xFBAEB4, 0xFBABFD), 1.0, 0xFBAEB4,
            "cream through pink to orchid, drifting"),
    MISTRAL("mistral", List.of(0xFFF2BE, 0xFBAEB4, 0xFBABFD), 3.0, 0xFBAEB4,
            "cream through pink to orchid");

    private final String presetName;
    private final @Nullable GlowEffect effect;
    private final @Nullable List<Integer> stops;
    private final double speed;
    private final TextColor chatColor;
    private final String description;

    EffectPreset(String presetName, GlowEffect effect, int chatColor, String description) {
        this(presetName, effect, null, 1.0, chatColor, description);
    }

    EffectPreset(String presetName, List<Integer> stops, double speed, int chatColor, String description) {
        this(presetName, null, List.copyOf(stops), speed, chatColor, description);
    }

    EffectPreset(String presetName, @Nullable GlowEffect effect, @Nullable List<Integer> stops,
                 double speed, int chatColor, String description) {
        this.presetName = presetName;
        this.effect = effect;
        this.stops = stops;
        this.speed = speed;
        this.chatColor = TextColor.color(chatColor);
        this.description = description;
    }

    public @NotNull String presetName() {
        return presetName;
    }

    public @NotNull GlowEffect effect() {
        if (effect != null) {
            return effect;
        }
        int index = GradientPalette.get().indexOf(presetName);
        if (index < 0) {

            return GlowEffect.solid(stops.getFirst());
        }
        return GlowEffect.palette(index, stops.getFirst());
    }

    public @Nullable List<Integer> stops() {
        return stops;
    }

    public double speed() {
        return speed;
    }

    public @NotNull TextColor chatColor() {
        return chatColor;
    }

    public @NotNull String description() {
        return description;
    }

    public static @NotNull Optional<EffectPreset> byName(@NotNull String name) {
        return Arrays.stream(values()).filter(p -> p.presetName.equalsIgnoreCase(name)).findFirst();
    }

    public static @NotNull List<String> names() {
        return Arrays.stream(values()).map(EffectPreset::presetName).toList();
    }
}
