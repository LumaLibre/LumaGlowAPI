package dev.lumas.glowapi.commands;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.lumacore.manager.commands.AbstractCommandManager;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@AutoRegister(RegisterType.COMMAND)
@CommandInfo(
        name = "lumaglowapi",
        aliases = {"lga", "glow"},
        description = "Base command for LumaGlowAPI",
        usage = "/<command> <subcommand>",
        permission = "lumaglowapi.command"
)
public class CommandManager extends AbstractCommandManager<LumaGlowAPI, SubCommand> {
    public CommandManager() {
        super(LumaGlowAPI.getInstance());
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (args.length == 0) {
            @SuppressWarnings("UnstableApiUsage")
            String version = LumaGlowAPI.getInstance().getPluginMeta().getVersion();
            String impl = GlowColorManager.getInstance().handler().getClass().getSimpleName();
            Text.msg(sender, String.format("v%s w/ %s implementation. <dark_gray>(/%s <subcommand>)", version, impl, label));
            return true;
        }
        return super.handle(sender, label, args);
    }
}
