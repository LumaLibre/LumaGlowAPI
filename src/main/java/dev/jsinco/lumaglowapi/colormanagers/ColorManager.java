package dev.jsinco.lumaglowapi.colormanagers;

import dev.jsinco.lumaglowapi.LumaGlowAPI;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ColorManager {
    private final static LumaGlowAPI plugin = LumaGlowAPI.getInstance();

    private final static Map<UUID, ChatColor> playerColors = new HashMap<>();
    private final static LinkedHashMap<String, ChatColor> defaultColorPermissionsList = new LinkedHashMap<>();

    public static void loadDefaultColorPermissions() {
        defaultColorPermissionsList.clear();
        for (String permissionOrKey : plugin.getConfig().getConfigurationSection("default-colors").getKeys(true)) {
            if (permissionOrKey.equals("group")) {
                continue;
            }
            defaultColorPermissionsList.put(permissionOrKey, ChatColor.valueOf(plugin.getConfig().getString("default-colors." + permissionOrKey)));
        }
    }

    public static boolean updatePlayersColor(Player player) {
        ChatColor color = null;

        if (player.getPersistentDataContainer().has(new NamespacedKey(plugin, "color"), PersistentDataType.STRING)) {
            color = ChatColor.valueOf(player.getPersistentDataContainer().get(new NamespacedKey(plugin, "color"), PersistentDataType.STRING));
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
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "color"), PersistentDataType.STRING, color.name());
        updatePlayersColor(player);
    }

    public static void removePlayerColor(Player player) {
        player.getPersistentDataContainer().remove(new NamespacedKey(plugin, "color"));
        updatePlayersColor(player);
    }

    @Nullable
    public static ChatColor getPlayerColor(Player player) {
        return playerColors.get(player.getUniqueId());
    }

    public static void clearPlayerColor(Player player) {
        playerColors.remove(player.getUniqueId());
    }
}
