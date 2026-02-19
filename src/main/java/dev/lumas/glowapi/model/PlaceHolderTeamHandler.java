package dev.lumas.glowapi.model;

import com.google.common.base.Preconditions;
import dev.lumas.glowapi.LumaGlowAPI;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaceHolderTeamHandler implements GlowColorHandler {

    private final GlowColorHandler entityDelegate;
    private final Map<UUID, TextColor> playerColors;

    public PlaceHolderTeamHandler(GlowColorHandler entityDelegate) {
        Preconditions.checkArgument(!(entityDelegate instanceof  PlaceHolderTeamHandler), "Entity delegate cannot be an instance of PlaceHolderTeamHandler");
        this.entityDelegate = entityDelegate;
        this.playerColors = new HashMap<>();
    }

    public PlaceHolderTeamHandler() {
        this(IS_FOLIA ? new PacketTeamHandler() : new BukkitTeamHandler());
    }

    @Override
    public void setColor(Entity entity, NamedTextColor color) {
        if (!(entity instanceof Player)) {
            entityDelegate.setColor(entity, color);
            return;
        }

        entity.getPersistentDataContainer().set(COLOR_KEY, PersistentDataType.STRING, color.toString());
        update(entity);
    }

    @Override
    public void setTransientColor(Entity entity, NamedTextColor color, @Nullable Long duration) {
        if (!(entity instanceof Player)) {
            entityDelegate.setTransientColor(entity, color, duration);
            return;
        }

        playerColors.put(entity.getUniqueId(), color);

        if (duration != null) {
            entity.getScheduler().runDelayed(LumaGlowAPI.getInstance(), (task) -> {
                TextColor laterColor = playerColors.get(entity.getUniqueId());
                if (laterColor != null && laterColor.equals(color)) {
                    update(entity);
                }
            }, null, duration);
        }
    }

    @Override
    public void removeColor(Entity entity) {
        if (!(entity instanceof Player)) {
            entityDelegate.removeColor(entity);
            return;
        }

        entity.getPersistentDataContainer().remove(COLOR_KEY);
        update(entity);
    }

    @Override
    public void update(Entity entity) {
        if (!(entity instanceof Player player)) {
            entityDelegate.update(entity);
            return;
        }

        NamedTextColor storedColor = getStoredColor(player);
        UUID playerUUID = player.getUniqueId();

        if (storedColor == null) {
            // try to go back to default color
            TextColor defaultColor = getDefaultColor(entity);
            if (defaultColor != null) {
                playerColors.put(playerUUID, defaultColor);
            } else {
                playerColors.remove(playerUUID);
            }
            return;
        }

        playerColors.put(playerUUID, storedColor);
    }

    @Override
    public @Nullable TextColor getColor(Entity entity) {
        if (!(entity instanceof Player player)) {
            return entityDelegate.getColor(entity);
        }

        TextColor color = playerColors.get(player.getUniqueId());
        if (color != null) {
            return color;
        }

        NamedTextColor storedColor = getStoredColor(player);
        if (storedColor != null) {
            return storedColor;
        }

        return getDefaultColor(player);
    }


    public @Nullable NamedTextColor getStoredColor(Player player) {
        String storedColor = player.getPersistentDataContainer().get(COLOR_KEY, PersistentDataType.STRING);
        if (storedColor != null) {
            return NamedTextColor.NAMES.value(storedColor);
        }
        return null;
    }

    @ApiStatus.Internal
    @Override
    public void shutdownHook() {
        entityDelegate.shutdownHook();
    }

    @ApiStatus.Internal
    @Override
    public void playerJoinHook(Player player) {
        entityDelegate.playerJoinHook(player);
    }

    @ApiStatus.Internal
    @Override
    public void playerQuitHook(Player player) {
        entityDelegate.playerQuitHook(player);
    }
}
