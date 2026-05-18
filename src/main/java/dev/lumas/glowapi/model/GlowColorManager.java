package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.config.Config;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.lumas.glowapi.model.GlowColorHandler.IS_FOLIA;

public record GlowColorManager(@Delegate GlowColorHandler handler) {

    private static GlowColorManager instance;

    @ApiStatus.Internal
    public static GlowColorManager newInstance() {
        if (instance != null) {
            instance.close();
        }

        Config config = LumaGlowAPI.getOkaeriConfig();
        GlowColorHandler delegate = IS_FOLIA || config.isPreferPacketBasedTeams() ? new PacketTeamHandler() : new BukkitTeamHandler();
        GlowColorHandler colorHandler = config.shouldUsePlaceHolders() ? new PlaceHolderTeamHandler(delegate) : delegate;

        instance = new GlowColorManager(colorHandler);
        return instance;
    }

    @NotNull
    public static GlowColorManager getInstance() {
        if (instance == null) {
            instance = newInstance();
        }
        return instance;
    }

    @Nullable
    @ApiStatus.Internal
    public static GlowColorManager getInstanceOrNull() {
        return instance;
    }
}
