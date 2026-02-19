package dev.lumas.glowapi.colormanagers;

import dev.lumas.glowapi.GlowColorManager;
import dev.lumas.glowapi.model.GlowColorHandler;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public final class ColorManager {

    private static final GlowColorHandler HANDLER = GlowColorManager.getInstance().handler();


    public static boolean updatePlayersColor(Player player) {
        HANDLER.update(player);
        return true;
    }

    public static void setPlayerColor(Player player, ChatColor color) {
        HANDLER.setColor(player, fromChatColor(color));
    }

    public static void setPlayerColor(Player player, NamedTextColor color) {
        HANDLER.setColor(player, color);
    }

    public static void setTempPlayerColor(Player player, ChatColor color) {
        HANDLER.setTransientColor(player, fromChatColor(color));
    }

    public static void setTempPlayerColor(Player player, NamedTextColor color) {
        HANDLER.setTransientColor(player, color);
    }

    public static void removePlayerColor(Player player) {
        HANDLER.removeColor(player);
    }

    @Nullable
    public static ChatColor getPlayerColor(Player player) {
        return fromNamedTextColor(HANDLER.getColor(player));
    }

    @Nullable
    public static NamedTextColor playerColor(Player player) {
        return (NamedTextColor) HANDLER.getColor(player);
    }

    public static void clearPlayerColor(Player player) {
        HANDLER.removeColor(player);
    }



    private static NamedTextColor fromChatColor(ChatColor color) {
        return NamedTextColor.NAMES.value(color.name().toLowerCase());
    }

    private static ChatColor fromNamedTextColor(TextColor color) {
        if (null == color) {
            return null;
        }
        return ChatColor.valueOf(color.toString().toUpperCase());
    }
}
