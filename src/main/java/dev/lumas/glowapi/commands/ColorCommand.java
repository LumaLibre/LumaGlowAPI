package dev.lumas.glowapi.commands;

import dev.lumas.glowapi.GlowColorManager;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "color",
        description = "Change the color of yourself or another player",
        usage = "/<command> color <color!> <player?>",
        permission = "lumaglowapi.command.color"
)
public class ColorCommand implements SubCommand {
    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender sender, String s, String[] args) {
        if (args.length == 0) {
            Text.msg(sender, "Specify a color.");
            return false;
        }

        String colorString = args[0];
        Player target = null;

        if (args.length > 1 && sender.hasPermission("lumaglowapi.command.color.others")) {
            target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        }

        if (target == null) {
            Text.msg(sender, "Player not found.");
            return true;
        }

        GlowColorManager manager = GlowColorManager.getInstance();

        if (colorString.equals("reset")) {
            manager.removeColor(target);
            manager.update(target);
            Text.msg(sender, "Color reset.");
            return true;
        }

        NamedTextColor color = NamedTextColor.NAMES.value(colorString);
        if (color == null) {
            Text.msg(sender, "Invalid color.");
            return true;
        }

        if (!sender.hasPermission("lumaglowapi.color." + colorString.toLowerCase())) {
            Text.msg(sender, "You don't have permission to use this color.");
            return true;
        }

        manager.setColor(target, color);
        Text.msg(sender, "Color set to " + colorString + ".");
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> NamedTextColor.NAMES.keys().stream().toList();
            case 2 -> {
                if (sender.hasPermission("lumaglowapi.command.color.others")) {
                    yield null; // Assume Bukkit will handle player name tab completion
                } else {
                    yield List.of();
                }
            }
            default -> List.of();
        };
    }
}
