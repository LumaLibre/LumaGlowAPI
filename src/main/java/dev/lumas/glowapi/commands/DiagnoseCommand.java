package dev.lumas.glowapi.commands;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.CommandMeta;
import dev.lumas.core.annotation.Register;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.glowapi.model.GlowStyleHandler;
import dev.lumas.glowapi.pack.PackService;
import dev.lumas.glowapi.papi.ColorPlaceholder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Register(Autowire.SUBCOMMAND)
@CommandMeta(
        name = "diagnose",
        description = "Inspect a player's glow style and marker without changing them",
        usage = "/<command> diagnose <player?>",
        permission = "lumaglowapi.command.diagnose",
        parent = CommandManager.class
)
public class DiagnoseCommand implements SubCommand {
    @Override
    public boolean execute(LumaGlowAPI plugin, CommandSender sender, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0])
                : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(Component.text("Specify an online player."));
            return true;
        }
        target.getScheduler().run(plugin, task -> {
            List<String> lines = new ArrayList<>();
            lines.add("Glow diagnostics for " + target.getName());
            if (GlowColorManager.getInstance().handler() instanceof GlowStyleHandler handler) {
                lines.addAll(handler.diagnostics(target));
            }
            String placeholder = new ColorPlaceholder().onPlaceholderRequest(target, "color");
            lines.add("TAB placeholder output: " + (placeholder == null || placeholder.isEmpty()
                    ? "empty (TAB controls formatting)" : placeholder.replace('\u00a7', '&')));
            lines.add("Server-generated pack SHA-1: " + PackService.getInstance().hash().orElse("not built"));
            lines.add("Assume pack loaded: " + LumaGlowAPI.getOkaeriConfig().getEffects().isAssumePackLoaded());
            var markers = target.getPassengers().stream()
                    .filter(entity -> entity.getScoreboardTags().contains(GlowStyleHandler.MARKER_TAG)).toList();
            if (sender instanceof Player viewer) {
                viewer.getScheduler().run(plugin, viewerTask -> {
                    for (var marker : markers) {
                        lines.add("Viewer can see marker " + marker.getUniqueId() + ": " + viewer.canSee(marker));
                    }
                    lines.add("Viewer reports a resource pack: " + viewer.hasResourcePack()
                            + " (does not verify spatial shaders in a merged pack)");
                    send(sender, lines);
                }, null);
            } else {
                send(sender, lines);
            }
        }, () -> sender.sendMessage(Component.text("Player left before diagnostics could run.")));
        return true;
    }

    private static void send(CommandSender sender, List<String> lines) {
        sender.sendMessage(Component.text(String.join("\n", lines)));
    }

    @Override
    public List<String> tabComplete(LumaGlowAPI plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}
