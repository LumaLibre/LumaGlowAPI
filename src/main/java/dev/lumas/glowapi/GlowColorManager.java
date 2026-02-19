package dev.lumas.glowapi;

import dev.lumas.glowapi.config.Config;
import dev.lumas.glowapi.model.BukkitTeamHandler;
import dev.lumas.glowapi.model.GlowColorHandler;
import dev.lumas.glowapi.model.PacketTeamHandler;
import dev.lumas.glowapi.model.PlaceHolderTeamHandler;
import lombok.experimental.Delegate;

public record GlowColorManager(@Delegate GlowColorHandler handler) {

    private static GlowColorManager instance;

    public static GlowColorManager newInstance() {
        if (instance != null) {
            instance.shutdownHook();
        }

        Config config = LumaGlowAPI.getOkaeriConfig();
        GlowColorHandler colorHandler;

        if (config.shouldUsePlaceHolders()) {
            colorHandler = new PlaceHolderTeamHandler();
        } else if (GlowColorHandler.IS_FOLIA || config.isPreferPacketBasedTeams()) {
            colorHandler = new PacketTeamHandler();
        } else {
            colorHandler = new BukkitTeamHandler();
        }

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
