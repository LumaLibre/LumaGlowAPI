package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.config.Config;
import lombok.experimental.Delegate;

import static dev.lumas.glowapi.model.GlowColorHandler.IS_FOLIA;

public record GlowColorManager(@Delegate GlowColorHandler handler) {

    private static GlowColorManager instance;

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

    public static GlowColorManager getInstance() {
        if (instance == null) {
            instance = newInstance();
        }
        return instance;
    }
}
