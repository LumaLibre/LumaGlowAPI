package dev.lumas.glowapi.commands;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;


@AutoRegister(RegisterType.SUBCOMMAND)
@CommandInfo(
        name = "reload",
        description = "Reload the config",
        usage = "/<command> reload",
        permission = "lumaglowapi.command.reload"
)
public class ReloadCommand implements SubCommand {
    @Override
    public boolean execute(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String s, String[] strings) {
        LumaGlowAPI.getOkaeriConfig().load(true);
        GlowColorManager.newInstance();
        Bukkit.getOnlinePlayers().forEach(player -> {
            GlowColorManager glowColorManager = GlowColorManager.getInstance();
            glowColorManager.playerJoinHook(player);
            glowColorManager.update(player);
        });
        Text.msg(commandSender, "Config reloaded! (New GlowManager instance)");
        return true;
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI lumaGlowAPI, CommandSender commandSender, String[] strings) {
        return List.of();
    }
}
