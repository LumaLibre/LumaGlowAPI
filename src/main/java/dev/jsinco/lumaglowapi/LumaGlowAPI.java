package dev.jsinco.lumaglowapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class LumaGlowAPI extends JavaPlugin implements Listener {

    private static LumaGlowAPI instance;
    private static ColorPlaceholder colorPlaceholder = null;

    @Override
    public void onEnable() {
        instance = this;
        getConfig().options().copyDefaults();
        saveDefaultConfig();


        ColorManager.loadDefaultColorPermissions();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ColorManager.updatePlayersColor(player);
        }


        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            colorPlaceholder = new ColorPlaceholder();
            colorPlaceholder.register();
        }

        getCommand("color").setExecutor(new ColorCommand());
        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ColorManager.updatePlayersColor(player);
            }
        }, 0, 100);
    }

    @Override
    public void onDisable() {
        if (colorPlaceholder != null) {
            colorPlaceholder.unregister();
        }
    }

    public static LumaGlowAPI getInstance() {
        return instance;
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ColorManager.updatePlayersColor(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ColorManager.clearPlayerColor(event.getPlayer());
    }
}
