package dev.lumas.glowapi.commands;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.glowapi.util.StringUtil;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "colorentity",
        aliases = {"colore"},
        description = "Color a targeted entity",
        usage = "/<command> colorentity <color!> -transient -glow",
        permission = "lumaglowapi.command.colorentity",
        parent = CommandManager.class
)
public class ColorEntityCommand implements SubCommand {

    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender sender, String s, String[] args) {
        List<String> argsList = List.of(args);

        if (argsList.isEmpty()) return false;

        Player player = (Player) sender;
        Entity entity = player.getTargetEntity(100);

        String stringColor = argsList.getFirst();

        if (entity == null) {
            Text.msg(sender, "Look at an entity.");
            return true;
        } else if (entity instanceof Player) {
            Text.msg(sender, "You can't color players with this command.");
            return true;
        }

        GlowColorManager manager = GlowColorManager.getInstance();

        if (stringColor.equals("reset")) {
            manager.removeColor(entity);
            Text.msg(sender, "Color reset.");
            return true;
        }

        NamedTextColor color = NamedTextColor.NAMES.value(stringColor);
        if (color == null) {
            Text.msg(sender, "Invalid color %s.".formatted(stringColor));
            return true;
        }

        doColor(sender, argsList, entity, stringColor, manager, color);

        if (argsList.contains("-glow")) {
            entity.setGlowing(!entity.isGlowing());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String[] args) {
        if (args.length == 1) {
            List<String> colors = new ArrayList<>(NamedTextColor.NAMES.keys().stream().toList());
            colors.add("reset");
            return colors;
        } else {
            return List.of("-transient", "-glow");
        }
    }

    static void doColor(CommandSender sender, List<String> argsList, Entity entity, String stringColor, GlowColorManager manager, NamedTextColor color) {
        Component component = Component.text("Color set to ")
                .append(Component.text(StringUtil.toProperCase(stringColor), color));

        if (argsList.contains("-transient")) {
            manager.setTransientColor(entity, color);
            Text.msg(sender, component.append(Component.text(" (transient).")));
        } else {
            manager.setColor(entity, color);
            Text.msg(sender, component.append(Component.text(".")));
        }
    }
}
