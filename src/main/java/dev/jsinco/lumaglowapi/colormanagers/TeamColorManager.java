package dev.jsinco.lumaglowapi.colormanagers;

import dev.jsinco.lumaglowapi.LumaGlowAPI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamColorManager {

    private final static String TEAM_PREFIX = "LGA-";
    private final static Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
    private final static Map<UUID, Integer> teamTickTasks = new HashMap<>();


    public static Team getColorTeam(NamedTextColor color) {
        Team team = board.getTeam(TEAM_PREFIX + color.toString());
        if (team == null) {
            team = board.registerNewTeam(TEAM_PREFIX + color);
            team.color(color);
        }
        return team;
    }

    public static void addTeamColor(Entity entity, NamedTextColor color) {
        Team team = getColorTeam(color);
        team.addEntry(entity.getUniqueId().toString());
    }

    public static void removeTeamColor(Entity entity) {
        Team team = board.getEntryTeam(entity.getUniqueId().toString());
        if (team != null) {
            team.removeEntry(entity.getUniqueId().toString());
        }
    }

    public static void addToTeamForTicksPersistent(Entity entity, NamedTextColor color, long ticks) {
        UUID uuid = entity.getUniqueId();
        if (teamTickTasks.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(teamTickTasks.get(uuid));
        }
        addTeamColor(entity, color);
        teamTickTasks.put(uuid, Bukkit.getScheduler().scheduleSyncDelayedTask(LumaGlowAPI.getInstance(), () -> {
            removeTeamColor(entity);
            teamTickTasks.remove(uuid);
        }, ticks));
    }

    public static void addToTeamForTicks(Entity entity, NamedTextColor color, long ticks) {
        addTeamColor(entity, color);
        Bukkit.getScheduler().scheduleSyncDelayedTask(LumaGlowAPI.getInstance(), () -> removeTeamColor(entity), ticks);
    }

    @Nullable
    public static ChatColor getTeamColor(Entity entity) {
        Team team = board.getEntryTeam(entity.getUniqueId().toString());
        if (team != null) {
            return team.getColor();
        }
        return null;
    }

}
