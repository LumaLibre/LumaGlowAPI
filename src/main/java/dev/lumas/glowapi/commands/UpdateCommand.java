package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.lumacore.utility.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "update",
        description = "Update color",
        usage = "/<command> <subcommand>",
        permission = "lumaglowapi.command.update",
        parent = CommandManager.class
)
public class UpdateCommand implements SubCommand {
    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender sender, String s, String[] args) {

        Player target;
        if (args.length == 0 && sender instanceof Player player) {
            target = player;
        } else if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
        } else {
            Text.msg(sender, "Specify a player.");
            return false;
        }

        GlowColorManager.getInstance().update(target);
        Text.msg(sender, "Updated.");
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String[] strings) {
        return null;
    }
}
