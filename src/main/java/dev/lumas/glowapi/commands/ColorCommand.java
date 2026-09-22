package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.util.Text;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.glowapi.model.GlowStyle;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "color",
        description = "Change the glow style of yourself or another player",
        usage = "/<command> color <color|preset|#rrggbb|effect|reset> <player?> -transient",
        permission = "lumaglowapi.command.color",
        parent = CommandManager.class
)
public class ColorCommand implements SubCommand {

    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender sender, String label, String[] strings) {
        List<String> args = List.of(strings);

        if (args.isEmpty()) {
            Text.msg(sender, "Specify a color, preset, #rrggbb or effect.");
            return false;
        }

        String styleString = args.getFirst().toLowerCase();

        Player target = args.size() > 1 && !args.get(1).startsWith("-") && sender.hasPermission("lumaglowapi.command.color.others")
                ? Bukkit.getPlayerExact(args.get(1))
                : null;

        if (target == null && sender instanceof Player player) {
            target = player;
        }

        if (target == null) {
            Text.msg(sender, "Player not found.");
            return true;
        }

        GlowColorManager manager = GlowColorManager.getInstance();

        if (styleString.equals("reset")) {
            manager.removeColor(target);
            Text.msg(sender, "Color reset.");
            return true;
        }

        GlowStyle style = GlowStyle.parse(styleString);
        if (style == null) {
            Text.msg(sender, "Invalid style %s. <gray>Use a color name, a preset, #rrggbb, rainbow:8, gradient:#rrggbb:4 or pulse:#rrggbb:8.".formatted(styleString));
            return true;
        }

        if (!sender.hasPermission(style.permission())) {
            Text.msg(sender, "You don't have permission to use this style.");
            return true;
        }

        ColorEntityCommand.doColor(sender, args, target, manager, style);
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> {
                List<String> styles = new ArrayList<>(GlowStyle.suggestions(sender::hasPermission));
                styles.add("reset");
                yield styles;
            }
            case 2 -> {
                List<String> list = new ArrayList<>(List.of("-transient"));

                if (sender.hasPermission("lumaglowapi.command.color.others")) {
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(list::add);
                }
                yield list;
            }
            default -> List.of("-transient");
        };
    }
}
