package dev.lumas.glowapi.model;

import dev.lumas.glowapi.effect.EffectPreset;
import dev.lumas.glowapi.effect.GlowEffect;
import dev.lumas.glowapi.effect.GradientPalette;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public sealed interface GlowStyle permits GlowStyle.Named, GlowStyle.Effect {

    record Named(@NotNull NamedTextColor color) implements GlowStyle {
        @Override
        public @NotNull String serialize() {
            return color.toString().toUpperCase(Locale.ROOT);
        }

        @Override
        public @NotNull TextColor displayColor() {
            return color;
        }

        @Override
        public @NotNull String describe() {
            return color.toString();
        }
    }

    record Effect(@NotNull GlowEffect effect, @Nullable EffectPreset preset) implements GlowStyle {
        @Override
        public @NotNull String serialize() {
            if (preset != null) {
                return preset.presetName();
            }
            String hex = String.format("#%06x", effect.rgb());
            return switch (effect.type()) {
                case SOLID -> hex;
                case RAINBOW -> "rainbow:" + effect.param();
                case GRADIENT -> effect.param() == GlowEffect.FULL_WHEEL ? "gradient:" + hex : "gradient:" + hex + ":" + effect.param();
                case PULSE -> "pulse:" + hex + ":" + effect.param();
                case PALETTE -> {

                    String name = GradientPalette.get().nameOf(effect.paletteIndex());
                    yield name != null ? name : hex;
                }
            };
        }

        @Override
        public @NotNull TextColor displayColor() {
            if (preset != null) {
                return preset.chatColor();
            }
            return effect.type() == GlowEffect.Type.RAINBOW ? TextColor.color(0xFF55FF) : TextColor.color(effect.rgb());
        }

        @Override
        public @NotNull String describe() {
            return preset != null ? preset.presetName() + " (" + preset.description() + ")" : effect.describe();
        }
    }

    static @NotNull GlowStyle of(@NotNull NamedTextColor color) {
        return new Named(color);
    }

    static @NotNull GlowStyle of(@NotNull GlowEffect effect) {
        return new Effect(effect, null);
    }

    static @NotNull GlowStyle of(@NotNull EffectPreset preset) {
        return new Effect(preset.effect(), preset);
    }

    static @Nullable GlowStyle parse(@NotNull String text) {
        String s = text.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        NamedTextColor named = NamedTextColor.NAMES.value(s);
        if (named != null) {
            return of(named);
        }
        EffectPreset preset = EffectPreset.byName(s).orElse(null);
        if (preset != null) {
            return of(preset);
        }

        int paletteIndex = GradientPalette.get().indexOf(s);
        if (paletteIndex >= 0) {
            GradientPalette.Gradient gradient = GradientPalette.get().byIndex(paletteIndex);
            return of(GlowEffect.palette(paletteIndex, gradient.stops().getFirst()));
        }
        if (s.startsWith("#")) {
            int rgb = GlowEffect.parseHex(s);
            return rgb < 0 ? null : of(GlowEffect.solid(rgb));
        }

        String[] parts = s.split(":");
        try {
            switch (parts[0]) {
                case "rainbow" -> {
                    if (parts.length > 2) return null;
                    return of(GlowEffect.rainbow(parts.length == 2 ? Integer.parseInt(parts[1]) : GlowEffect.DEFAULT_SPEED));
                }
                case "solid" -> {
                    if (parts.length != 2) return null;
                    int rgb = GlowEffect.parseHex(parts[1]);
                    return rgb < 0 ? null : of(GlowEffect.solid(rgb));
                }
                case "gradient" -> {
                    if (parts.length < 2 || parts.length > 3) return null;
                    int rgb = GlowEffect.parseHex(parts[1]);
                    if (rgb < 0) return null;
                    return of(GlowEffect.gradient(rgb, parts.length == 3 ? Integer.parseInt(parts[2]) : GlowEffect.FULL_WHEEL));
                }
                case "pulse" -> {
                    if (parts.length < 2 || parts.length > 3) return null;
                    int rgb = GlowEffect.parseHex(parts[1]);
                    if (rgb < 0) return null;
                    return of(GlowEffect.pulse(rgb, parts.length == 3 ? Integer.parseInt(parts[2]) : GlowEffect.DEFAULT_SPEED));
                }
                default -> {
                    return null;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static @NotNull List<String> suggestions() {
        List<String> list = new ArrayList<>(NamedTextColor.NAMES.keys());
        list.addAll(EffectPreset.names());
        list.addAll(GradientPalette.get().names());
        list.add("#ff8800");
        list.add("rainbow:8");
        list.add("gradient:#ff8800:4");
        list.add("pulse:#00ffcc:8");
        return list;
    }

    static @NotNull List<String> suggestions(@NotNull Predicate<String> hasPermission) {
        return suggestions().stream()
                .filter(suggestion -> {
                    GlowStyle style = parse(suggestion);
                    return style != null && hasPermission.test(style.permission());
                })
                .toList();
    }

    @NotNull String serialize();

    @NotNull TextColor displayColor();

    @NotNull String describe();

    default boolean requiresPack() {
        return this instanceof Effect;
    }

    default @NotNull String permission() {
        if (this instanceof Named n) {
            return "lumaglowapi.color." + n.color();
        }
        Effect e = (Effect) this;
        if (e.preset() != null) {
            return "lumaglowapi.color.preset." + e.preset().presetName();
        }
        if (e.effect().type() == GlowEffect.Type.PALETTE) {
            String name = GradientPalette.get().nameOf(e.effect().paletteIndex());
            if (name != null) {
                return "lumaglowapi.color.gradient." + name;
            }
        }
        return "lumaglowapi.color.custom";
    }
}
