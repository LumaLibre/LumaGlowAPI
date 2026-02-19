package dev.lumas.glowapi.commands;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.lumacore.manager.commands.AbstractCommandManager;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;

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
}
