package dev.lumas.glowapi.papi;

import dev.lumas.glowapi.GlowColorManager;
import dev.lumas.glowapi.config.Config;
import dev.lumas.glowapi.model.GlowColorHandler;
import dev.lumas.glowapi.model.PlaceHolderTeamHandler;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.manager.placeholder.PlaceholderInfo;
import dev.lumas.lumacore.manager.placeholder.SoloAbstractPlaceholder;
import dev.lumas.glowapi.LumaGlowAPI;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoRegister(RegisterType.PLACEHOLDER)
@PlaceholderInfo(
        identifier = "lumaglowapi",
        author = "Jsinco",
        version = "2.0.0"
)
public class ColorPlaceholder extends SoloAbstractPlaceholder {

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        Config config = LumaGlowAPI.getOkaeriConfig();

        TextColor color;

        if (!player.isGlowing()) {
            if (config.isOnlyShowPlaceholderWhenGlowing()) {
                return "";
            }
            color = config.getColorToShowWhileNotGlowing();
        } else {
            GlowColorHandler handler = GlowColorManager.getInstance().handler();
            if (!(handler instanceof PlaceHolderTeamHandler)) {
                return "";
            }
            color = handler.getColor(player);
        }


        if (color != null) {
            @SuppressWarnings("deprecation")
            String chatColor = ChatColor.translateAlternateColorCodes('&', toSimple(color));
            return chatColor;
        }
        return "";
    }


    private String toSimple(TextColor textColor) {
        if (!(textColor instanceof NamedTextColor namedTextColor)) {
            return "&f";
        }

        return switch (namedTextColor.toString()) {
            case "aqua" -> "&b";
            case "black" -> "&0";
            case "blue" -> "&9";
            case "dark_aqua" -> "&3";
            case "dark_blue" -> "&1";
            case "dark_gray" -> "&8";
            case "dark_green" -> "&2";
            case "dark_purple" -> "&5";
            case "dark_red" -> "&4";
            case "gold" -> "&6";
            case "gray" -> "&7";
            case "green" -> "&a";
            case "light_purple" -> "&d";
            case "red" -> "&c";
            case "yellow" -> "&e";
            default -> "&f";
        };
    }
}
