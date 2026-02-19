package dev.lumas.glowapi.config;

import dev.lumas.glowapi.util.ClassUtil;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class Config extends OkaeriConfig {

    @CustomKey("always-use-teams")
    @Comment({
            "Whether we should always use teams for glow colors on players or use",
            "PAPI placeholders when TAB is present on the server. (Only applies to players)"
    })
    private boolean alwaysUseTeams = false;

    @CustomKey("only-show-placeholder-when-glowing")
    @Comment({
            "Only show a color from the PAPI placeholder when the player is glowing.",
            "If false, the placeholder will return an empty string when the player is not",
            "glowing."
    })
    private boolean onlyShowPlaceholderWhenGlowing = true;


    @CustomKey("prefer-packet-based-teams")
    @Comment("Whether to prefer packet-based teams (using ScoreboardLibrary) over regular Bukkit teams.")
    private boolean preferPacketBasedTeams = false;


    @CustomKey("color-to-show-while-not-glowing")
    @Comment({
            "The color to show in the PAPI placeholder when the player is not glowing.",
            "Only applies if 'only-show-placeholder-when-glowing' is false.",
            "Must be a valid color name from the NamedTextColor enum (e.g. WHITE, RED, BLUE, etc.)"
    })
    private NamedTextColor colorToShowWhileNotGlowing = NamedTextColor.WHITE;


    @CustomKey("default-colors")
    @Comment({
            "Default colors by permission",
            "This will be the default color of the player if they have no set color",
            "Priority goes from top down, whatever permission is found first will be the color"
    })
    private Map<String, NamedTextColor> defaultColors = Map.of(
            "group.admin", NamedTextColor.RED,
            "group.mod", NamedTextColor.LIGHT_PURPLE,
            "group.helper", NamedTextColor.GREEN,
            "group.content", NamedTextColor.RED,
            "group.ethereal", NamedTextColor.LIGHT_PURPLE,
            "group.seraphic", NamedTextColor.GREEN,
            "group.luminal", NamedTextColor.DARK_RED,
            "group.divine", NamedTextColor.AQUA,
            "group.arcane", NamedTextColor.RED,
            "group.default", NamedTextColor.GRAY
    );


    public boolean shouldUsePlaceHolders() {
        return !alwaysUseTeams && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
