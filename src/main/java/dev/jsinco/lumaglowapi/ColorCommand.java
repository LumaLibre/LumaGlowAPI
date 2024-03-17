package dev.jsinco.lumaglowapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ColorCommand implements TabExecutor {


    private final LumaGlowAPI plugin = LumaGlowAPI.getInstance();


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /color <color>");
            return true;
        }

        boolean reset = false;

        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("lumaglowapi.reload")) {
            plugin.reloadConfig();
            ColorManager.loadDefaultColorPermissions();
            for (Player player : Bukkit.getOnlinePlayers()) {
                ColorManager.updatePlayersColor(player);
            }
            sender.sendMessage("Config reloaded!");
            return true;
        } else if (args[0].equalsIgnoreCase("reset")) {
            reset = true;
        }

        Player player;
        if (args.length >= 2 && sender.hasPermission("lumaglowapi.colorother")) {
            player = plugin.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("Player not found!");
                return true;
            }
        } else if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage("You must be a player to use this command or specify a player! Usage: /color <color> <player>");
            return true;
        }

        if (reset) {
            ColorManager.removePlayerColor(player);
            return true;
        }

        ChatColor color;
        try {
            color = ChatColor.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Invalid color!");
            return true;
        }

        if (!player.hasPermission("lumaglowapi.color." + color.name().toLowerCase())) {
            sender.sendMessage("You do not have permission to use that color!");
            return true;
        }

        ColorManager.setPlayerColor(player, color);
        sender.sendMessage("Color set to " + color + color.name().toLowerCase());
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> tabCompletion = new ArrayList<>();

            for (ChatColor color : ChatColor.values()) {
                if (!color.isColor()) continue;
                tabCompletion.add(color.name().toLowerCase());
            }
            if (sender.hasPermission("lumaglowapi.reload")) {
                tabCompletion.add("reload");
            }

            return tabCompletion;
        }
        return null;
    }
}
