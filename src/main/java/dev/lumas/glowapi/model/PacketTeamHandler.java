package dev.lumas.glowapi.model;

import com.google.common.base.Preconditions;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.lumacore.utility.ContextLogger;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.megavex.scoreboardlibrary.api.team.ScoreboardTeam;
import net.megavex.scoreboardlibrary.api.team.TeamDisplay;
import net.megavex.scoreboardlibrary.api.team.TeamManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class PacketTeamHandler implements GlowColorHandler {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();

    private final TeamManager teamManager;
    private final String teamNameFormat;
    private final String transientTeamNameFormat;
    private final Set<ScoreboardTeam> transientTeams;

    public PacketTeamHandler(@NotNull String teamNameFormat, @NotNull String transientTeamNameFormat) {
        Preconditions.checkArgument(teamNameFormat.contains("%s"), "Team name format must contain a %s for the color name");
        Preconditions.checkArgument(transientTeamNameFormat.contains("%s"), "Transient team name format must contain a %s for the color name");
        this.teamManager = LumaGlowAPI.getScoreboardLibrary().createTeamManager();
        this.teamNameFormat = teamNameFormat;
        this.transientTeamNameFormat = transientTeamNameFormat;
        this.transientTeams = new HashSet<>();
    }

    public PacketTeamHandler(@NotNull String teamNameFormat) {
        this(teamNameFormat, "LGAT-%s");
    }

    public PacketTeamHandler() {
        this("LGA-%s");
    }

    @Override
    public void setColor(Entity entity, NamedTextColor color) {
        entity.getPersistentDataContainer().set(COLOR_KEY, PersistentDataType.STRING, color.toString());

        ScoreboardTeam applicableTeam = getApplicableTeam(entity);
        if (applicableTeam != null) {
            TeamDisplay display = applicableTeam.defaultDisplay();
            display.removeEntry(entity.getUniqueId().toString());
        }
        ScoreboardTeam team = getTeam(color);
        team.defaultDisplay().addEntry(entity.getUniqueId().toString());
    }

    @Override
    public void setTransientColor(Entity entity, NamedTextColor color, @Nullable Long duration) {
        ScoreboardTeam applicableTeam = getApplicableTeam(entity);
        if (applicableTeam != null) {
            TeamDisplay display = applicableTeam.defaultDisplay();
            display.removeEntry(entity.getUniqueId().toString());
        }

        ScoreboardTeam team = getTransientTeam(color);
        team.defaultDisplay().addEntry(entity.getUniqueId().toString());

        if (duration != null) {
            entity.getScheduler().runDelayed(LumaGlowAPI.getInstance(), (task) -> {
                ScoreboardTeam currentTeam = getApplicableTeam(entity);
                if (currentTeam != null && currentTeam.equals(team)) {
                    currentTeam.defaultDisplay().removeEntry(entity.getUniqueId().toString());
                    update(entity);
                }
            }, null, duration);
        }
    }

    @Override
    public void removeColor(Entity entity) {
        ScoreboardTeam applicableTeam = getApplicableTeam(entity);
        if (applicableTeam != null) {
            TeamDisplay display = applicableTeam.defaultDisplay();
            display.removeEntry(entity.getUniqueId().toString());
        }

        entity.getPersistentDataContainer().remove(COLOR_KEY);
        update(applicableTeam, entity);
    }

    @Override
    public void update(Entity entity) {
        ScoreboardTeam applicableTeam = getApplicableTeam(entity);
        update(applicableTeam, entity);
    }

    @Override
    public @Nullable TextColor getColor(Entity entity) {
        ScoreboardTeam applicableTeam = getApplicableTeam(entity);
        return getColor(applicableTeam, entity);
    }


    private void update(ScoreboardTeam applicable, Entity entity) {
        if (applicable != null && transientTeams.contains(applicable)) {
            applicable.defaultDisplay().removeEntry(entity.getUniqueId().toString());
        }

        TextColor color = getColor(applicable, entity);
        if (color instanceof NamedTextColor namedColor) {
            getTeam(namedColor).defaultDisplay().addEntry(entity.getUniqueId().toString());
        } else if (color != null) {
            LOGGER.error("Bad color " + color + " for entity " + entity);
        }
    }

    private @Nullable TextColor getColor(@Nullable ScoreboardTeam applicable, Entity entity) {
        if (applicable != null) {
            return applicable.defaultDisplay().playerColor();
        }

        TextColor storedColor = getStoredColor(entity);
        if (storedColor != null) {
            return storedColor;
        }

        return getDefaultColor(entity);
    }


    private @Nullable ScoreboardTeam getApplicableTeam(Entity entity) {
        for (ScoreboardTeam team : teamManager.teams()) {
            if (team.defaultDisplay().entries().contains(entity.getUniqueId().toString())) {
                return team;
            }
        }
        return null;
    }


    private @NotNull ScoreboardTeam getTeam(NamedTextColor color) {
        return getOrRegisterTeam(teamNameFormat, color);
    }


    private @NotNull ScoreboardTeam getTransientTeam(NamedTextColor color) {
        ScoreboardTeam team = getOrRegisterTeam(transientTeamNameFormat, color);
        transientTeams.add(team);
        return team;
    }

    private @NotNull ScoreboardTeam getOrRegisterTeam(String nameFormat, NamedTextColor color) {
        String teamName = nameFormat.formatted(color.toString());

        ScoreboardTeam team = teamManager.team(teamName);

        if (team == null) {
            team = teamManager.createIfAbsent(teamName);
            TeamDisplay display = team.defaultDisplay();
            display.playerColor(color);
        }
        return team;
    }



    private @Nullable NamedTextColor getStoredColor(Entity entity) {
        String storedColor = entity.getPersistentDataContainer().get(COLOR_KEY, PersistentDataType.STRING);
        if (storedColor != null) {
            return NamedTextColor.NAMES.value(storedColor);
        }
        return null;
    }

    @ApiStatus.Internal
    @Override
    public void playerJoinHook(Player player) {
        teamManager.addPlayer(player);
    }

    @ApiStatus.Internal
    @Override
    public void playerQuitHook(Player player) {
        teamManager.removePlayer(player);
    }

    @ApiStatus.Internal
    @Override
    public void shutdownHook() {
        teamManager.close();
    }
}
