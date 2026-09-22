package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.util.Text;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.pack.PackService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "pack",
        description = "Re-send the glow-effect resource pack to yourself or another player",
        usage = "/<command> pack <player?>",
        permission = "lumaglowapi.command.pack",
        parent = CommandManager.class
)
public class PackCommand implements SubCommand {

    @Override
    public boolean execute(LumaGlowAPI plugin, CommandSender sender, String label, String[] args) {
        PackService pack = PackService.getInstance();
        if (!pack.deliverable()) {
            Text.msg(sender, "<yellow>The effect pack is not being delivered. Set effects.pack-url or enable "
                    + "effects.pack-server in config.yml; the zip is in plugins/LumaGlowAPI/.");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                Text.msg(sender, "Player not found.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            Text.msg(sender, "Specify a player.");
            return false;
        }

        pack.send(target);
        Text.msg(sender, "Pack re-sent to " + target.getName() + " <dark_gray>(" + pack.url().orElse("?") + ")");
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}
