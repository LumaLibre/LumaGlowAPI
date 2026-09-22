package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.command.AbstractCommand;
import dev.lumas.glowapi.LumaGlowAPI;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Register(Autowire.COMMAND)
@CommandMeta(
        name = "color",
        description = "Change the glow style of yourself or another player",
        usage = "/<command> <color|preset|#rrggbb|effect|reset> <player?> -transient",
        permission = "lumaglowapi.command.color"
)
public class ColorAliasCommand extends AbstractCommand {

    private final ColorCommand delegate = new ColorCommand();

    @Override
    public boolean handle(CommandSender sender, String label, String[] args) {
        return delegate.execute(LumaGlowAPI.getInstance(), sender, label, args);
    }

    @Override
    public @Nullable List<String> handleTabComplete(CommandSender sender, String label, String[] args) {
        return delegate.tabComplete(LumaGlowAPI.getInstance(), sender, args);
    }
}
