package dev.jsinco.lumaglowapi.colormanagers;

import dev.jsinco.lumaglowapi.LumaGlowAPI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ColorManager {
    private final static NamedTextColor DEFAULT = NamedTextColor.WHITE;
    private final static LumaGlowAPI plugin = LumaGlowAPI.getInstance();

    private final static Map<UUID, NamedTextColor> playerColors = new HashMap<>();
    private final static LinkedHashMap<String, NamedTextColor> defaultColorPermissionsList = new LinkedHashMap<>();

    public static void loadDefaultColorPermissions() {
        defaultColorPermissionsList.clear();
        for (String permissionOrKey : plugin.getConfig().getConfigurationSection("default-colors").getKeys(true)) {
            if (permissionOrKey.equals("group")) {
                continue;
            }
            defaultColorPermissionsList.put(permissionOrKey, NamedTextColor.NAMES.valueOr(plugin.getConfig().getString("default-colors." + permissionOrKey), DEFAULT));
        }
    }

    public static boolean updatePlayersColor(Player player) {
        NamedTextColor color = null;

        if (player.getPersistentDataContainer().has(new NamespacedKey(plugin, "color"), PersistentDataType.STRING)) {
            color = NamedTextColor.NAMES.valueOr(player.getPersistentDataContainer().get(new NamespacedKey(plugin, "color"), PersistentDataType.STRING), DEFAULT);
        } else {
            for (String permission : defaultColorPermissionsList.keySet()) {
                if (player.hasPermission(permission)) {
                    color = defaultColorPermissionsList.get(permission);
                    break;
                }
            }
        }

        if (color != null) {
            playerColors.put(player.getUniqueId(), color);

            if (plugin.getConfig().getBoolean("use-teams")) {
                TeamColorManager.addTeamColor(player, color);
            }
            return true;
        }
        return false;
    }

    public static void setPlayerColor(Player player, ChatColor color) {
        NamedTextColor namedColor = NamedTextColor.NAMES.valueOr(color.name(), DEFAULT);
        setPlayerColor(player, namedColor);
    }

    public static void setTempPlayerColor(Player player, ChatColor color) {
        NamedTextColor namedColor = NamedTextColor.NAMES.valueOr(color.name(), DEFAULT);
        playerColors.put(player.getUniqueId(), namedColor);
    }

    public static void setPlayerColor(Player player, NamedTextColor color) {
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "color"), PersistentDataType.STRING, color.toString());
        updatePlayersColor(player);
    }

    public static void setTempPlayerColor(Player player, NamedTextColor color) {
        playerColors.put(player.getUniqueId(), color);
    }

    public static void removePlayerColor(Player player) {
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "color"));
        updatePlayersColor(player);
    }

    @Nullable
    public static NamedTextColor playerColor(Player player) {
        return playerColors.get(player.getUniqueId());
    }

    @Nullable
    public static ChatColor getPlayerColor(Player player) {
        NamedTextColor color = playerColors.get(player.getUniqueId());
        if (color != null) {
            return ChatColor.valueOf(color.toString());
        } else {
            return null;
        }
    }

    public static void clearPlayerColor(Player player) {
        playerColors.remove(player.getUniqueId());
    }
}
