package dev.jsinco.lumaglowapi;

import dev.jsinco.luma.lumacore.manager.modules.ModuleManager;
import dev.jsinco.lumaglowapi.colormanagers.ColorManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LumaGlowAPI extends JavaPlugin {

    private static LumaGlowAPI instance;
    private static ModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        getConfig().options().copyDefaults();
        saveDefaultConfig();


        ColorManager.loadDefaultColorPermissions();
        Bukkit.getOnlinePlayers().forEach(ColorManager::updatePlayersColor);


        moduleManager.reflectivelyRegisterModules();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ColorManager.updatePlayersColor(player);
            }
        }, 0, 100);
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();
    }

    public static LumaGlowAPI getInstance() {
        return instance;
    }


    public static ChatColor chatColorFromString(String color) {
        try {
            return ChatColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatColor.WHITE;
        }
    }

    public static String getColorCodeByChatColor(ChatColor color) {
        if (color == null) {
            return "&f";
        }
        return switch (color) {
            case AQUA -> "&b";
            case BLACK -> "&0";
            case BLUE -> "&9";
            case DARK_AQUA -> "&3";
            case DARK_BLUE -> "&1";
            case DARK_GRAY -> "&8";
            case DARK_GREEN -> "&2";
            case DARK_PURPLE -> "&5";
            case DARK_RED -> "&4";
            case GOLD -> "&6";
            case GRAY -> "&7";
            case GREEN -> "&a";
            case LIGHT_PURPLE -> "&d";
            case RED -> "&c";
            case YELLOW -> "&e";
            default -> "&f";
        };
    }

}
