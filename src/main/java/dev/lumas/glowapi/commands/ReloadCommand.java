package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.glowapi.pack.PackService;
import dev.lumas.lumacore.utility.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;


@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "reload",
        description = "Reload the config",
        usage = "/<command> reload",
        permission = "lumaglowapi.command.reload",
        parent = CommandManager.class
)
public class ReloadCommand implements SubCommand {
    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String s, String[] strings) {
        LumaGlowAPI.getOkaeriConfig().load(true);
        GlowColorManager.newInstance();
        Bukkit.getOnlinePlayers().forEach(player -> {
            GlowColorManager glowColorManager = GlowColorManager.getInstance();
            glowColorManager.addPlayer(player);
        });

        PackService pack = PackService.getInstance();
        pack.reload();
        if (pack.deliverable()) {
            if (commandSender instanceof Player player) {
                pack.send(player);
            }
            Text.msg(commandSender, "Reloaded. Pack served from " + pack.url().orElseThrow()
                    + " <dark_gray>(/" + s + " pack to push it to others)");
        } else {
            Text.msg(commandSender, "Reloaded.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
