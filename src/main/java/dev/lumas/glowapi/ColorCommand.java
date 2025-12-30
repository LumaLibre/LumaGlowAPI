package dev.lumas.glowapi;

import dev.lumas.lumacore.manager.commands.AbstractCommand;
import dev.lumas.lumacore.manager.commands.CommandInfo;
import dev.lumas.lumacore.manager.modules.AutoRegister;
import dev.lumas.lumacore.manager.modules.RegisterType;
import dev.lumas.lumacore.utility.Text;
import dev.lumas.glowapi.colormanagers.ColorManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


@AutoRegister(RegisterType.COMMAND)
@CommandInfo(
        name = "color",
        description = "Change your color or the color of another player",
        usage = "/<command> <color!> <player?>",
        permission = "lumaglowapi.color"
)
public class ColorCommand extends AbstractCommand {


    // Terrible code
    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return false;
        }

        boolean reset = false;

        String arg = args[0];

        if (arg.equalsIgnoreCase("reload") && sender.hasPermission("lumaglowapi.reload")) {
            LumaGlowAPI.getInstance().reloadConfig();
            ColorManager.loadDefaultColorPermissions();
            Bukkit.getOnlinePlayers().forEach(ColorManager::updatePlayersColor);
            Text.msg(sender, "Config reloaded!");
            return true;
        } else if (arg.equalsIgnoreCase("reset")) {
            reset = true;
        }

        Player player;
        if (args.length >= 2 && sender.hasPermission("lumaglowapi.colorother")) {
            player = Bukkit.getPlayerExact(args[1]);
            if (player == null) {
                Text.msg(sender, "Player not found!");
                return true;
            }
        } else if (sender instanceof Player patternVar) {
            player = patternVar;
        } else {
            Text.msg(sender,"You must be a player to use this command or specify a player! Usage: /color <color> <player>");
            return true;
        }

        if (reset) {
            ColorManager.removePlayerColor(player);
            Text.msg(sender, "Player " + player.getName() + " has been reset!");
            return true;
        }

        ChatColor color;
        try {
            color = ChatColor.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException e) {
            Text.msg(sender,"Invalid color!");
            return true;
        }
        String colorName = color.name().toLowerCase();

        if (!player.hasPermission("lumaglowapi.color." + colorName)) {
            Text.msg(sender,"You do not have permission to use that color!");
            return true;
        }

        ColorManager.setPlayerColor(player, color);

        Text.msg(sender, "Color set to <" + colorName + ">" + colorName);
        return true;
    }

    @Override
    public @Nullable List<String> handleTabComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            return null;
        }
        List<String> tabCompletion = new ArrayList<>();

        for (ChatColor color : ChatColor.values()) {
            if (!color.isColor()) continue;
            tabCompletion.add(color.name().toLowerCase());
        }
        if (sender.hasPermission("lumaglowapi.reload")) {
            tabCompletion.add("reload");
        }
        tabCompletion.add("reset");

        return tabCompletion;
    }
}
