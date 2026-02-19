package dev.lumas.glowapi.colormanagers;

import dev.lumas.glowapi.GlowColorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public final class ColorManager {

    private static final GlowColorManager DELEGATE_HANDLER = GlowColorManager.getInstance();


    public static boolean updatePlayersColor(Player player) {
        DELEGATE_HANDLER.update(player);
        return true;
    }

    public static void setPlayerColor(Player player, ChatColor color) {
        DELEGATE_HANDLER.setColor(player, fromChatColor(color));
    }

    public static void setPlayerColor(Player player, NamedTextColor color) {
        DELEGATE_HANDLER.setColor(player, color);
    }

    public static void setTempPlayerColor(Player player, ChatColor color) {
        DELEGATE_HANDLER.setTransientColor(player, fromChatColor(color));
    }

    public static void setTempPlayerColor(Player player, NamedTextColor color) {
        DELEGATE_HANDLER.setTransientColor(player, color);
    }

    public static void removePlayerColor(Player player) {
        DELEGATE_HANDLER.removeColor(player);
    }

    @Nullable
    public static ChatColor getPlayerColor(Player player) {
        return fromNamedTextColor(DELEGATE_HANDLER.getColor(player));
    }

    @Nullable
    public static NamedTextColor playerColor(Player player) {
        return (NamedTextColor) DELEGATE_HANDLER.getColor(player);
    }

    public static void clearPlayerColor(Player player) {
        DELEGATE_HANDLER.removeColor(player);
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
