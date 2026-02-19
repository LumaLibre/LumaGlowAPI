package dev.lumas.glowapi.commands;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "color",
        description = "Change the color of yourself or another player",
        usage = "/<command> color <color!> <player?> -transient",
        permission = "lumaglowapi.command.color",
        parent = CommandManager.class
)
public class ColorCommand implements SubCommand {
    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender sender, String s, String[] strings) {
        List<String> args = List.of(strings);

        if (args.isEmpty()) {
            Text.msg(sender, "Specify a color.");
            return false;
        }

        String colorString = args.getFirst().toLowerCase();

        Player target = args.size() > 1 && sender.hasPermission("lumaglowapi.command.color.others")
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

        if (colorString.equals("reset")) {
            manager.removeColor(target);
            Text.msg(sender, "Color reset.");
            return true;
        }

        NamedTextColor color = NamedTextColor.NAMES.value(colorString);
        if (color == null) {
            Text.msg(sender, "Invalid color %s.".formatted(colorString));
            return true;
        }

        if (!sender.hasPermission("lumaglowapi.color." + colorString)) {
            Text.msg(sender, "You don't have permission to use this color.");
            return true;
        }

        Component component = Component.text("Color set to ")
                .append(Component.text(toProperCase(colorString), color));

        if (args.contains("-transient")) {
            manager.setTransientColor(target, color);
            Text.msg(sender, component.append(Component.text(" (transient).")));
        } else {
            manager.setColor(target, color);
            Text.msg(sender, component.append(Component.text(".")));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> {
                List<String> colors = new ArrayList<>(NamedTextColor.NAMES.keys().stream().toList());
                colors.add("reset");
                yield colors;
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

    private String toProperCase(String s) {
        return Arrays.stream(s.split("_"))
                .map(w -> w.substring(0,1).toUpperCase() + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
