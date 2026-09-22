package dev.lumas.glowapi.config;

import dev.lumas.glowapi.effect.EffectPreset;
import dev.lumas.glowapi.model.GlowStyle;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
                "If true, the placeholder returns an empty string when the player is not glowing.",
                "If false, it returns color-to-show-while-not-glowing."
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
            "Default glow styles by permission",
            "This will be the default style of the player if they have no set style",
            "A style is a team color (RED), a preset (fire, ocean, pulse_red ...), or an RGB color",
            "(\"#ff8800\"), or a custom effect: rainbow:8, gradient:#ff8800:4, pulse:#00ffcc:8.",
            "Anything other than a team color needs the resource pack (see effects below)."
    })
    private Map<String, GlowStyle> defaultColors = new LinkedHashMap<>() {{
        put("group.admin", GlowStyle.of(NamedTextColor.RED));
        put("group.mod", GlowStyle.of(NamedTextColor.LIGHT_PURPLE));
        put("group.helper", GlowStyle.of(NamedTextColor.GREEN));
        put("group.content", GlowStyle.of(NamedTextColor.LIGHT_PURPLE));
        put("group.mistral", GlowStyle.of(EffectPreset.MISTRAL));
        put("group.ethereal", GlowStyle.of(NamedTextColor.LIGHT_PURPLE));
        put("group.seraphic", GlowStyle.of(NamedTextColor.GREEN));
        put("group.luminal", GlowStyle.of(NamedTextColor.DARK_RED));
        put("group.divine", GlowStyle.of(NamedTextColor.AQUA));
        put("group.arcane", GlowStyle.of(NamedTextColor.RED));
        put("group.default", GlowStyle.of(NamedTextColor.GRAY));
    }};


    @CustomKey("effects")
    @Comment({
        "Custom shader glow effects."
    })
    private Effects effects = new Effects();

    @Getter
    public static class Effects extends OkaeriConfig {

        @Comment("Enable or disable this")
        private boolean enabled = true;

        @CustomKey("pack-url")
        @Comment({
                "Where clients download the pack from."
        })
        private String packUrl = "";

        @CustomKey("pack-required")
        @Comment("Kick players who decline the pack.")
        private boolean packRequired = false;

        @CustomKey("send-pack-on-join")
        private boolean sendPackOnJoin = false;

        @CustomKey("pack-prompt")
        @Comment("Shown in the client's resource pack prompt.")
        private String packPrompt = "<gray>LumaGlowAPI needs a small shader pack for animated glow effects.";

        @CustomKey("gradients")
        @Comment({
                "Ex:",
                "  gradients:",
                "    sunrise:",
                "      - \"#fff2be\"",
                "      - \"#fbaeb4\"",
                "      - \"#fbabfd\"",
                "",
                "Add a \"speed:<number>\" entry to set how fast it travels; 1.0 is the default rate.",
                "",
                "(resourcepack). Up to 16 colours each."
        })
        private Map<String, List<String>> gradients = new LinkedHashMap<>();

        @CustomKey("spatial-identification")
        @Comment({
                "Experimental location-based effects with independently chosen native nametag colors.",
                "Ambiguous neighborhoods or lookup collisions fall back to ordinary team-color glow.",
                "Requires rebuilding and redistributing the pack after changing this setting.",
                "See docs/spatial-effects.md for compatibility, performance and visual tradeoffs."
        })
        private boolean spatialIdentification = false;

        @CustomKey("nametag-colors")
        @Comment({
                "Preferred native team/nametag colors for custom effects; ordinary Minecraft colors stay available.",
                "Keys: a style name or expression, rgb, gradients, or * for the default.",
                "Example: mistral: WHITE, gradients: GRAY, rgb: WHITE, '*': WHITE.",
                "TAB must continue using the LumaGlowAPI color placeholder for its team color.",
                "Nearby conflicting effects use gray, then light-to-dark alternatives when needed.",
                "Non-glowing players follow the normal placeholder settings."
        })
        private Map<String, NamedTextColor> nametagColors = new LinkedHashMap<>(Map.of("*", NamedTextColor.WHITE));

        public NamedTextColor nametagColor(GlowStyle.Effect style) {
            String name = style.serialize().toLowerCase(Locale.ROOT);
            String category = switch (style.effect().type()) {
                case SOLID -> "rgb";
                case GRADIENT, PALETTE -> "gradients";
                default -> "*";
            };
            NamedTextColor exact = null;
            NamedTextColor grouped = null;
            NamedTextColor fallback = NamedTextColor.WHITE;
            for (var entry : nametagColors.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                String key = entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (key.equals(name)) exact = entry.getValue();
                if (key.equals(category)) grouped = entry.getValue();
                if (key.equals("*")) fallback = entry.getValue();
            }
            return exact != null ? exact : grouped != null ? grouped : fallback;
        }

        @CustomKey("assume-pack-loaded")
        @Comment({
                "Normally the effect marker is only shown to clients that report a resource pack as",
                "loaded; anyone else sees nothing (a client without the pack would render the marker",
                "as a missing-texture square). Set true only if every client is guaranteed to have the",
                "generated pack's contents by some other route, e.g. merged into another pack."
        })
        private boolean assumePackLoaded = false;

        @CustomKey("debug-overlay")
        @Comment({
                "Replaces the glow colours with a diagnostic, for working out why an effect looks",
                "wrong. Rebuild and re-send the pack after changing it. What you then see:",
                "  hue ramp    body height, yellow at the feet through green to blue at the head.",
                "  red         no marker data for this silhouette's id.",
                "  magenta     a silhouette the outline pass did not recognise as an id colour.",
                "Marker data lives in a reserved framebuffer row and is never drawn as an outline.",
                "Spatial mode also shows a framed table in the upper left: green cells contain complete",
                "marker entries, yellow cells are incomplete, and an empty frame means no entries arrived.",
                "Spatial outline colors: red = missing entry, orange = location collision, magenta = team mismatch."
        })
        private boolean debugOverlay = false;

        @CustomKey("marker-gap")
        @Comment({
                "Legacy offset of the invisible marker model relative to the entity's feet."
        })
        private double markerGap = -0.5;

        @CustomKey("pack-server")
        private PackServer packServer = new PackServer();
    }

    @Getter
    public static class PackServer extends OkaeriConfig {

        private boolean enabled = false;

        @CustomKey("bind-address")
        private String bindAddress = "0.0.0.0";

        private int port = 25580;

        @CustomKey("public-address")
        @Comment("The address clients will download from. Must be reachable from the client.")
        private String publicAddress = "localhost";
    }

    public boolean useAssignedPlaceholderColor(boolean glowing) {
        return glowing;
    }

    public boolean shouldUsePlaceHolders() {
        return !alwaysUseTeams && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
