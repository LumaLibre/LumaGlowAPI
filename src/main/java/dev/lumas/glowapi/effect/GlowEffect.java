package dev.lumas.glowapi.effect;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GlowEffect {

    static final int MARKER = 0x20;

    public static final int DEFAULT_SPEED = 8;
    public static final int FULL_WHEEL = 0;

    public enum Type {

        RAINBOW(0),

        GRADIENT(1),

        PULSE(2),

        SOLID(3),

        PALETTE(4);

        final int mode;

        Type(int mode) {
            this.mode = mode;
        }
    }

    private final Type type;
    private final int param;
    private final int rgb;

    private final int paletteIndex;

    private GlowEffect(Type type, int param, int rgb) {
        this(type, param, rgb, -1);
    }

    private GlowEffect(Type type, int param, int rgb, int paletteIndex) {
        this.type = type;
        this.param = param;
        this.rgb = rgb & 0xFFFFFF;
        this.paletteIndex = paletteIndex;
    }

    public static @NotNull GlowEffect rainbow() {
        return rainbow(DEFAULT_SPEED);
    }

    public static @NotNull GlowEffect rainbow(int speed) {
        return new GlowEffect(Type.RAINBOW, nibble(speed, "speed"), 0);
    }

    public static @NotNull GlowEffect solid(int rgb) {
        return new GlowEffect(Type.SOLID, 0, rgb);
    }

    public static @NotNull GlowEffect solid(@NotNull TextColor color) {
        return solid(color.value());
    }

    public static @NotNull GlowEffect gradient(int rgb) {
        return gradient(rgb, FULL_WHEEL);
    }

    public static @NotNull GlowEffect gradient(int rgb, int span) {
        return new GlowEffect(Type.GRADIENT, span == FULL_WHEEL ? 0 : nibble(span, "span"), rgb);
    }

    public static @NotNull GlowEffect gradient(@NotNull TextColor color, int span) {
        return gradient(color.value(), span);
    }

    public static @NotNull GlowEffect pulse(int rgb) {
        return pulse(rgb, DEFAULT_SPEED);
    }

    public static @NotNull GlowEffect pulse(int rgb, int speed) {
        return new GlowEffect(Type.PULSE, nibble(speed, "speed"), rgb);
    }

    public static @NotNull GlowEffect pulse(@NotNull TextColor color, int speed) {
        return pulse(color.value(), speed);
    }

    public static @NotNull GlowEffect palette(int paletteIndex, int displayRgb) {
        if (paletteIndex < 0 || paletteIndex >= GradientPalette.MAX_GRADIENTS) {
            throw new IllegalArgumentException("palette index out of range: " + paletteIndex);
        }
        return new GlowEffect(Type.PALETTE, 0, displayRgb, paletteIndex);
    }

    private static int nibble(int value, String what) {
        if (value < 1 || value > 15) {
            throw new IllegalArgumentException(what + " must be 1..15, got " + value);
        }
        return value;
    }

    public @NotNull Type type() {
        return type;
    }

    public int param() {
        return param;
    }

    public int rgb() {
        return rgb;
    }

    public int paletteIndex() {
        return paletteIndex;
    }

    public int encode() {
        int g;
        int b;
        if (type == Type.PALETTE) {

            g = (((paletteIndex >> 8) & 0xF) << 4) | type.mode;
            b = paletteIndex & 0xFF;
        } else if (type == Type.SOLID) {
            g = (((rgb >> 20) & 0xF) << 4) | type.mode;
            b = (((rgb >> 12) & 0xF) << 4) | ((rgb >> 4) & 0xF);
        } else {
            g = ((param & 0xF) << 4) | type.mode;
            b = hueByte(rgb);
        }
        return (MARKER << 16) | (g << 8) | b;
    }

    public int encode(int id) {
        return (encode() & 0xF0FFFF) | ((id & 0xF) << 16);
    }

    public static int hueByte(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        if (delta == 0f) {
            return 0;
        }
        float hue;
        if (max == r) {
            hue = ((g - b) / delta) % 6f;
        } else if (max == g) {
            hue = (b - r) / delta + 2f;
        } else {
            hue = (r - g) / delta + 4f;
        }
        hue /= 6f;
        if (hue < 0f) {
            hue += 1f;
        }
        return Math.round(hue * 255f) & 0xFF;
    }

    public static int parseHex(@NotNull String s) {
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public @NotNull String describe() {
        String hex = String.format("#%06x", rgb);
        return switch (type) {
            case RAINBOW -> "rainbow, speed " + param;
            case SOLID -> "solid " + hex;
            case GRADIENT -> "gradient from " + hex + ", span " + (param == 0 ? "full wheel" : param + "/16");
            case PULSE -> "pulse " + hex + ", speed " + param;
            case PALETTE -> {
                String name = GradientPalette.get().nameOf(paletteIndex);
                yield name == null ? "gradient #" + paletteIndex : "gradient " + name;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GlowEffect other && type == other.type && param == other.param
                && rgb == other.rgb && paletteIndex == other.paletteIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, param, rgb, paletteIndex);
    }

    @Override
    public String toString() {
        return "GlowEffect[" + describe() + "]";
    }
}
