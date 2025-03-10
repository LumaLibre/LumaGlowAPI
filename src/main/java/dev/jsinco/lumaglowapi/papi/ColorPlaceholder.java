package dev.jsinco.lumaglowapi.papi;

import dev.jsinco.luma.lumacore.manager.modules.AutoRegister;
import dev.jsinco.luma.lumacore.manager.modules.RegisterType;
import dev.jsinco.luma.lumacore.manager.placeholder.PlaceholderInfo;
import dev.jsinco.luma.lumacore.manager.placeholder.SoloAbstractPlaceholder;
import dev.jsinco.lumaglowapi.LumaGlowAPI;
import dev.jsinco.lumaglowapi.colormanagers.ColorManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "lumaglowapi",
        author = "Jsinco",
        version = "1.0.0"
)
public class ColorPlaceholder extends SoloAbstractPlaceholder {

    private static final LumaGlowAPI plugin = LumaGlowAPI.getInstance();

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        if (!player.isGlowing()) {
            if (plugin.getConfig().getBoolean("placeholder-only-show-while-glowing")) {
                return "";
            }
            String colorShowWhileNotGlowing = plugin.getConfig().getString("color-to-show-while-not-glowing");
            if (colorShowWhileNotGlowing != null) {
                ChatColor chatColor = LumaGlowAPI.chatColorFromString(colorShowWhileNotGlowing);
                return ChatColor.translateAlternateColorCodes('&', LumaGlowAPI.getColorCodeByChatColor(chatColor));
            }
        }
        if (plugin.getConfig().getBoolean("placeholder-only-show-while-glowing") && !player.isGlowing()) {
            return "";
        }

        ChatColor color = ColorManager.getPlayerColor(player);
        if (color != null) {
            return ChatColor.translateAlternateColorCodes('&', LumaGlowAPI.getColorCodeByChatColor(color));
        }
        return "";
    }
}
