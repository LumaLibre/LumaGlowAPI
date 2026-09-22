package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.util.ClassUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface GlowColorHandler {

    NamespacedKey COLOR_KEY = new NamespacedKey(LumaGlowAPI.getInstance(), "color");
    boolean IS_FOLIA = ClassUtil.classExists("io.papermc.paper.threadedregions.RegionizedServer");
    String TEAM_FORMAT = "lumaglowapi_%s";
    String TRANSIENT_TEAM_FORMAT = "lumaglowapi_transient_%s";

    void setColor(Entity entity, NamedTextColor color);

    void setTransientColor(Entity entity, NamedTextColor color, @Nullable Long duration);

    void removeColor(Entity entity);

    /** Clears the transient override and reapplies the saved style or permission default. */
    void update(Entity entity);

    @Nullable TextColor getColor(Entity entity);

    default void setTransientColor(Entity entity, NamedTextColor color) {
        setTransientColor(entity, color, null);
    }

    default void setStyle(@NotNull Entity entity, @NotNull GlowStyle style) {
        if (style instanceof GlowStyle.Named(NamedTextColor color)) {
            setColor(entity, color);
        } else {
            throw new UnsupportedOperationException("Effect styles need a GlowStyleHandler. Use GlowColorManager");
        }
    }

    default void setTransientStyle(@NotNull Entity entity, @NotNull GlowStyle style, @Nullable Long duration) {
        if (style instanceof GlowStyle.Named(NamedTextColor color)) {
            setTransientColor(entity, color, duration);
        } else {
            throw new UnsupportedOperationException("Effect styles need a GlowStyleHandler. Use GlowColorManager");
        }
    }

    default void setTransientStyle(@NotNull Entity entity, @NotNull GlowStyle style) {
        setTransientStyle(entity, style, null);
    }

    default @Nullable GlowStyle getStyle(@NotNull Entity entity) {
        TextColor color = getColor(entity);
        return color instanceof NamedTextColor named ? GlowStyle.of(named) : null;
    }

    default @Nullable GlowStyle getDefaultStyle(@NotNull Entity entity) {
        Map<String, GlowStyle> defaults = LumaGlowAPI.getOkaeriConfig().getDefaultColors();
        for (var entry : defaults.entrySet()) {
            if (entity.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    default @Nullable NamedTextColor getDefaultColor(Entity entity) {
        return getDefaultStyle(entity) instanceof GlowStyle.Named named ? named.color() : null;
    }

    default void setDefaultColor(Entity entity) {
        NamedTextColor defaultColor = getDefaultColor(entity);
        if (defaultColor != null) {
            setColor(entity, defaultColor);
        }
    }

    /**
     * The innermost handler, unwrapping decorators such as {@link GlowStyleHandler}.
     */
    default @NotNull GlowColorHandler root() {
        return this;
    }

    @ApiStatus.Internal
    default void close() {
        // no-op
    }

    @ApiStatus.Internal
    default void addPlayer(Player player) {
        // no-op
    }

    @ApiStatus.Internal
    default void removePlayer(Player player) {
        // no-op
    }
}
