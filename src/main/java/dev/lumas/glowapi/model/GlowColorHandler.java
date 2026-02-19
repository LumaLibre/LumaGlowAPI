package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.util.ClassUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface GlowColorHandler {

    NamespacedKey COLOR_KEY = new NamespacedKey(LumaGlowAPI.getInstance(), "color");
    boolean IS_FOLIA = ClassUtil.classExists("io.papermc.paper.threadedregions.RegionizedServer");

    void setColor(Entity entity, NamedTextColor color);

    void setTransientColor(Entity entity, NamedTextColor color, @Nullable Long duration);

    void removeColor(Entity entity);

    void update(Entity entity);

    @Nullable TextColor getColor(Entity entity);

    default void setTransientColor(Entity entity, NamedTextColor color) {
        setTransientColor(entity, color, null);
    }

    default @Nullable TextColor getDefaultColor(Entity entity) {
        Map<String, NamedTextColor> defaultColors = LumaGlowAPI.getOkaeriConfig().getDefaultColors();
        for (var entry : defaultColors.entrySet()) {
            if (entity.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    default void setDefaultColor(Entity entity) {
        TextColor defaultColor = getDefaultColor(entity);
        if (defaultColor != null) {
            setColor(entity, (NamedTextColor) defaultColor);
        }
    }

    @ApiStatus.Internal
    default void shutdownHook() {
        // no-op
    }

    @ApiStatus.Internal
    default void playerJoinHook(Player player) {
        // no-op
    }

    @ApiStatus.Internal
    default void playerQuitHook(Player player) {
        // no-op
    }
}
