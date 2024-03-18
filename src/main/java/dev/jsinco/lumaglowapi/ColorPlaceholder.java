package dev.jsinco.lumaglowapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorPlaceholder extends PlaceholderExpansion {

    private final LumaGlowAPI plugin = LumaGlowAPI.getInstance();

    @Override
    public @NotNull String getIdentifier() {
        return "lumaglowapi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jsinco";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || (plugin.getConfig().getBoolean("placeholder-only-show-while-glowing") && !player.isGlowing())) {
            return "";
        }

        ChatColor color = ColorManager.getPlayerColor(player);
        if (color != null) {
            return ChatColor.translateAlternateColorCodes('&', LumaGlowAPI.getColorCodeByChatColor(color));
        }
        return "";
    }
}
