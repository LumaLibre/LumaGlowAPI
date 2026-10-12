package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.util.Text;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.config.Config;
import dev.lumas.glowapi.model.MarkerTracking;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "tracking",
        description = "Show or set whether effect markers ride players or follow them",
        usage = "/<command> tracking <ride|follow?>",
        permission = "lumaglowapi.command.tracking",
        parent = CommandManager.class
)
public class TrackingCommand implements SubCommand {

    @Override
    public boolean execute(LumaGlowAPI plugin, CommandSender sender, String label, String[] args) {
        Config config = LumaGlowAPI.getOkaeriConfig();
        if (args.length == 0) {
            Text.msg(sender, "Effect markers currently use " + config.getEffects().getMarkerTracking()
                    + " tracking. <dark_gray>(/" + label + " tracking <ride|follow>)");
            return true;
        }

        MarkerTracking tracking;
        try {
            tracking = MarkerTracking.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Text.msg(sender, "Unknown tracking mode. Use ride or follow.");
            return true;
        }

        config.getEffects().setMarkerTracking(tracking);
        config.save();
        Text.msg(sender, "Effect markers now use " + tracking + " tracking.");
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? Arrays.stream(MarkerTracking.values())
                .map(mode -> mode.name().toLowerCase(Locale.ROOT)).toList() : List.of();
    }
}
