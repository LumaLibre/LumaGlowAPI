package dev.lumas.glowapi.model;

import com.google.common.base.Preconditions;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.lumacore.utility.ContextLogger;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link GlowColorHandler} using real Bukkit teams/scoreboard.
 * This is the default implementation on non-folia based servers for non-player entities.
 */
public class BukkitTeamHandler implements GlowColorHandler {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();

    private final Scoreboard board;
    private final String teamNameFormat;
    private final String transientTeamNameFormat;
    private final Set<Team> transientTeams;

    public BukkitTeamHandler(@NotNull String teamNameFormat, @NotNull String transientTeamNameFormat) {
        Preconditions.checkArgument(teamNameFormat.contains("%s"), "Team name format must contain a %s for the color name");
        Preconditions.checkArgument(transientTeamNameFormat.contains("%s"), "Transient team name format must contain a %s for the color name");

        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
        this.teamNameFormat = teamNameFormat;
        this.transientTeamNameFormat = transientTeamNameFormat;
        this.transientTeams = new HashSet<>();
    }

    public BukkitTeamHandler(@NotNull String teamNameFormat) {
        this(teamNameFormat, "LGAT-%s");
    }

    public BukkitTeamHandler() {
        this("LGA-%s");
    }

    @Override
    public void setColor(Entity entity, NamedTextColor color) {
        Team team = getTeam(color);
        team.addEntity(entity);
    }

    @Override
    public void setTransientColor(Entity entity, NamedTextColor color, @Nullable Long duration) {
        Team team = getTransientTeam(color);
        team.addEntity(entity);

        if (duration != null) {
            entity.getScheduler().runDelayed(LumaGlowAPI.getInstance(), (task) -> {
                team.removeEntity(entity);
                setDefaultColor(entity);
            }, null, duration);
        }
    }

    @Override
    public void removeColor(Entity entity) {
        Team team = board.getEntityTeam(entity);
        if (team != null) {
            team.removeEntity(entity);
        }
        setDefaultColor(entity);
    }

    @Override
    public void update(Entity entity) { // Consistent behavior with other implementations
        Team team = board.getEntityTeam(entity);
        if (team != null && transientTeams.contains(team)) {
            team.removeEntity(entity);
        }
        setDefaultColor(entity);
    }

    @Override
    public @Nullable TextColor getColor(Entity entity) {
        Team team = board.getEntityTeam(entity);
        if (team != null) {
            return team.color();
        }
        return null;
    }


    private Team getTeam(NamedTextColor color) {
        String teamName = teamNameFormat.formatted(color.toString());
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.color(color);
        }
        return team;
    }


    private Team getTransientTeam(NamedTextColor color) {
        String teamName = transientTeamNameFormat.formatted(color.toString());
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.color(color);
            transientTeams.add(team);
        }

        if (!transientTeams.contains(team)) {
            LOGGER.warning("Transient team " + team.getName() + " already exists but is not registered as a transient team.");
            transientTeams.add(team);
        }
        return team;
    }

    @ApiStatus.Internal
    @Override
    public void shutdownHook() {
        for (NamedTextColor color : NamedTextColor.NAMES.values()) {
            String teamName = teamNameFormat.formatted(color.toString());
            Team team = board.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
        for (Team team : transientTeams) {
            team.unregister();
        }
        transientTeams.clear();
    }
}
