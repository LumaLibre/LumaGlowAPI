package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.util.Text;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.glowapi.model.GlowStyle;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "colorentity",
        aliases = {"colore"},
        description = "Color a targeted entity",
        usage = "/<command> colorentity <color|preset|#rrggbb|effect|reset> -transient -glow",
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

        String styleString = argsList.getFirst().toLowerCase();

        if (entity == null) {
            Text.msg(sender, "Look at an entity.");
            return true;
        } else if (entity instanceof Player) {
            Text.msg(sender, "You can't color players with this command.");
            return true;
        }

        GlowColorManager manager = GlowColorManager.getInstance();

        if (styleString.equals("reset")) {
            manager.removeColor(entity);
            Text.msg(sender, "Color reset.");
            return true;
        }

        GlowStyle style = GlowStyle.parse(styleString);
        if (style == null) {
            Text.msg(sender, "Invalid style %s.".formatted(styleString));
            return true;
        }

        doColor(sender, argsList, entity, manager, style);

        if (argsList.contains("-glow")) {
            entity.setGlowing(!entity.isGlowing());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String[] args) {
        if (args.length == 1) {
            List<String> styles = new ArrayList<>(GlowStyle.suggestions(commandSender::hasPermission));
            styles.add("reset");
            return styles;
        } else {
            return List.of("-transient", "-glow");
        }
    }

    static void doColor(CommandSender sender, List<String> argsList, Entity entity, GlowColorManager manager, GlowStyle style) {
        Component component = Component.text("Style set to ")
                .append(Component.text(style.describe(), style.displayColor()));

        if (argsList.contains("-transient")) {
            manager.setTransientStyle(entity, style);
            Text.msg(sender, component.append(Component.text(" (transient).")));
        } else {
            manager.setStyle(entity, style);
            Text.msg(sender, component.append(Component.text(".")));
        }

    }
}
