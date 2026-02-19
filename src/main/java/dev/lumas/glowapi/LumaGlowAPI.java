package dev.lumas.glowapi;

import dev.lumas.glowapi.config.Config;
import dev.lumas.glowapi.config.NamedTextColorTransformer;
import dev.lumas.glowapi.model.GlowColorManager;
import dev.lumas.lumacore.manager.modules.ModuleManager;
import dev.lumas.lumacore.utility.ContextLogger;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class LumaGlowAPI extends JavaPlugin {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();

    @Getter
    private static LumaGlowAPI instance;
    private static ModuleManager moduleManager;
    @Getter
    private static Config okaeriConfig;
    @Getter
    private static ScoreboardLibrary scoreboardLibrary;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        okaeriConfig = loadOkaeriFile(Config.class, "config.yml");

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            scoreboardLibrary = new NoopScoreboardLibrary();
            LOGGER.error("Using NoopScoreboardLibrary.");
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            GlowColorManager manager = GlowColorManager.getInstance();
            manager.playerJoinHook(player);
            manager.update(player);
        });

        moduleManager.reflectivelyRegisterModules();
    }

    @Override
    public void onDisable() {
        moduleManager.unregisterModules();
        GlowColorManager.getInstance().shutdownHook();
        scoreboardLibrary.close();
    }

    public <T extends OkaeriConfig> T loadOkaeriFile(Class<T> clazz, String fileName) {
        return ConfigManager.create(clazz, cfg -> {
            cfg.configure(it -> {
                it.configurer(new YamlSnakeYamlConfigurer(), new StandardSerdes());
                it.removeOrphans(true);
                it.bindFile(this.getDataPath().resolve(fileName));
                it.serdes(new NamedTextColorTransformer());
            });
            cfg.saveDefaults();
            cfg.load(true);
        });
    }

}
