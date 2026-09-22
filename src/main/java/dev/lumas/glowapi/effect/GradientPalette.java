package dev.lumas.glowapi.effect;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GradientPalette {

    public static final int MAX_GRADIENTS = 4096;

    public static final int MAX_STOPS = 16;

    public record Gradient(@NotNull String name, @NotNull List<Integer> stops, double speed) {
    }

    public static final double DEFAULT_SPEED = 1.0;
    private static final double MAX_SPEED = 15.0;

    private static GradientPalette instance;

    private final List<Gradient> gradients;
    private final Map<String, Integer> indexByName;

    private GradientPalette(List<Gradient> gradients) {
        this.gradients = List.copyOf(gradients);
        Map<String, Integer> byName = new LinkedHashMap<>();
        for (int i = 0; i < gradients.size(); i++) {
            byName.put(gradients.get(i).name(), i);
        }
        this.indexByName = Collections.unmodifiableMap(byName);
    }

    public static @NotNull GradientPalette get() {
        if (instance == null) {
            if (LumaGlowAPI.getOkaeriConfig() == null) {
                return build();
            }
            instance = build();
        }
        return instance;
    }

    public static @NotNull GradientPalette rebuild() {
        instance = build();
        return instance;
    }

    private static GradientPalette build() {
        Map<String, Gradient> collected = new LinkedHashMap<>();

        for (EffectPreset preset : EffectPreset.values()) {
            List<Integer> stops = preset.stops();
            if (stops != null) {
                collected.put(preset.presetName(), new Gradient(preset.presetName(), stops, preset.speed()));
            }
        }

        Config config = LumaGlowAPI.getOkaeriConfig();
        Map<String, List<String>> configured = config == null ? null : config.getEffects().getGradients();
        if (configured != null) {
            for (Map.Entry<String, List<String>> entry : configured.entrySet()) {
                String name = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
                Gradient gradient = parseGradient(name, entry.getValue());
                if (gradient != null) {
                    collected.put(name, gradient);
                }
            }
        }

        List<Gradient> gradients = new ArrayList<>(collected.size());
        for (Map.Entry<String, Gradient> entry : collected.entrySet()) {
            if (gradients.size() >= MAX_GRADIENTS) {
                LumaGlowAPI.getInstance().getSLF4JLogger().warn(
                        "More than {} gradients defined; ignoring '{}' and any after it.", MAX_GRADIENTS, entry.getKey());
                break;
            }
            gradients.add(entry.getValue());
        }
        return new GradientPalette(gradients);
    }

    private static @Nullable Gradient parseGradient(String name, @Nullable List<String> raw) {
        var logger = LumaGlowAPI.getInstance().getSLF4JLogger();
        if (name.isEmpty()) {
            logger.warn("Ignoring a gradient in effects.gradients with an empty name.");
            return null;
        }
        if (raw == null || raw.isEmpty()) {
            logger.warn("Gradient '{}' lists no colours; ignoring it.", name);
            return null;
        }
        if (raw.size() > MAX_STOPS) {
            logger.warn("Gradient '{}' has {} colours, more than the {} supported; ignoring it.",
                    name, raw.size(), MAX_STOPS);
            return null;
        }
        List<Integer> stops = new ArrayList<>(raw.size());
        double speed = DEFAULT_SPEED;
        for (String hex : raw) {
            String token = hex == null ? "" : hex.trim();

            if (token.toLowerCase(Locale.ROOT).startsWith("speed:")) {
                try {
                    speed = Math.clamp(Double.parseDouble(token.substring(6).trim()), 0.0, MAX_SPEED);
                } catch (NumberFormatException e) {
                    logger.warn("Gradient '{}' has an unreadable speed '{}'; using {}.", name, token, DEFAULT_SPEED);
                }
                continue;
            }
            int rgb = GlowEffect.parseHex(token);
            if (rgb < 0) {
                logger.warn("Gradient '{}' has an invalid colour '{}' (expected #rrggbb); ignoring the gradient.",
                        name, hex);
                return null;
            }
            stops.add(rgb);
        }
        if (stops.isEmpty()) {
            logger.warn("Gradient '{}' lists a speed but no colours; ignoring it.", name);
            return null;
        }
        return new Gradient(name, List.copyOf(stops), speed);
    }

    public int indexOf(@NotNull String name) {
        Integer index = indexByName.get(name.toLowerCase(Locale.ROOT));
        return index == null ? -1 : index;
    }

    public @Nullable Gradient byIndex(int index) {
        return index < 0 || index >= gradients.size() ? null : gradients.get(index);
    }

    public @Nullable String nameOf(int index) {
        Gradient gradient = byIndex(index);
        return gradient == null ? null : gradient.name();
    }

    public @NotNull List<Gradient> all() {
        return gradients;
    }

    public @NotNull List<String> names() {
        return List.copyOf(indexByName.keySet());
    }

    public int size() {
        return gradients.size();
    }
}
