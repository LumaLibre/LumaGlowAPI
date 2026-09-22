package dev.lumas.glowapi.pack;

import dev.lumas.core.annotation.Autowire;
import dev.lumas.core.annotation.Register;
import dev.lumas.core.model.Service;
import dev.lumas.core.util.ContextLogger;
import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.config.Config;
import dev.lumas.glowapi.effect.GradientPalette;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.BuiltResourcePack;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Register(Autowire.SERVICE)
public final class PackService implements Service {

    private static final ContextLogger LOGGER = ContextLogger.getLogger();

    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("lumaglowapi:pack".getBytes(StandardCharsets.UTF_8));
    public static final String PACK_FILE = "pack.zip";

    private static PackService instance;

    private volatile BuiltResourcePack built;
    private volatile String url;
    private volatile PackServer server;

    private final AtomicInteger generation = new AtomicInteger();

    public PackService() {
        instance = this;
    }

    public static @NotNull PackService getInstance() {
        if (instance == null) {
            instance = new PackService();
        }
        return instance;
    }

    @Override
    public void register() {
        start();
    }

    @Override
    public void unregister() {
        stop();
    }

    public void reload() {
        stop();
        start();
    }

    private void start() {
        LumaGlowAPI plugin = LumaGlowAPI.getInstance();
        Config.Effects config = LumaGlowAPI.getOkaeriConfig().getEffects();
        if (!config.isEnabled()) {
            LOGGER.info("Shader glow effects are disabled (effects.enabled).");
            return;
        }

        GradientPalette palette = GradientPalette.rebuild();

        Path zipPath = plugin.getDataPath().resolve(PACK_FILE);
        int token = generation.incrementAndGet();

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            publish(token, config, zipPath, palette, false);
        } else {
            LOGGER.info("Players are online; building the effect pack off the main thread.");
            Bukkit.getAsyncScheduler().runNow(plugin, task -> publish(token, config, zipPath, palette, true));
        }
    }

    private void publish(int token, Config.Effects config, Path zipPath, GradientPalette palette, boolean async) {
        BuiltResourcePack result;
        try {
            ResourcePack pack = GlowPack.build(LumaGlowAPI.getInstance().getClass().getClassLoader(),
                    palette, config.isDebugOverlay());
            result = MinecraftResourcePackWriter.minecraft().build(pack);
            Files.createDirectories(zipPath.getParent());
            Files.write(zipPath, result.data().toByteArray());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not build the glow-effect resource pack; effects will not render.", e);
            return;
        }

        if (generation.get() != token) {
            return;
        }
        built = result;
        LOGGER.info("Built " + zipPath + " (sha1 " + result.hash() + ", " + palette.size() + " gradients)"
                + (config.isDebugOverlay() ? " [DEBUG OVERLAY ON]" : ""));

        if (!resolveDelivery(token, config, zipPath, result)) {
            return;
        }
        if (async && config.isSendPackOnJoin()) {

            sendToEveryone();
        }
    }

    private boolean resolveDelivery(int token, Config.Effects config, Path zipPath, BuiltResourcePack result) {
        String configured = config.getPackUrl();
        boolean configuredSet = configured != null && !configured.isBlank();
        String valid = configuredSet ? normalizeUrl(configured) : null;

        if (configuredSet && valid == null) {
            LOGGER.error("effects.pack-url is not a usable URL: '" + configured.trim() + "'. It must be the full "
                    + "http(s) address of the zip, e.g. http://example.com/pack.zip. Ignoring it.");
        }

        if (valid != null) {
            url = valid;
            LOGGER.info("Pack delivery: " + url + " (host " + zipPath.getFileName() + " there)");
            return true;
        }

        if (config.getPackServer().isEnabled()) {
            Config.PackServer serverConfig = config.getPackServer();
            try {
                PackServer started = new PackServer(result.data().toByteArray());
                started.start(serverConfig.getBindAddress(), serverConfig.getPort());
                if (generation.get() != token) {
                    started.stop();
                    return false;
                }
                server = started;
                url = "http://" + serverConfig.getPublicAddress() + ":" + serverConfig.getPort() + "/" + PACK_FILE;
                LOGGER.info("Pack delivery: built-in server at " + url);
                return true;
            } catch (IOException e) {
                LOGGER.error("Could not start the built-in pack server on "
                        + serverConfig.getBindAddress() + ":" + serverConfig.getPort(), e);
                server = null;
                return false;
            }
        }

        LOGGER.warning("Effect pack built but not being delivered: set effects.pack-url, or enable "
                + "effects.pack-server. Styles other than a plain team colour will not render.");
        return false;
    }

    private void sendToEveryone() {
        LumaGlowAPI plugin = LumaGlowAPI.getInstance();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(plugin, task -> send(player), null);
            }
        });
    }

    private void stop() {
        generation.incrementAndGet();
        PackServer running = server;
        if (running != null) {
            running.stop();
            server = null;
        }
        url = null;
        built = null;
    }

    private static @Nullable String normalizeUrl(String configured) {
        try {
            URI uri = new URI(configured.trim());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public boolean deliverable() {
        return built != null && url != null;
    }

    public @NotNull Optional<String> url() {
        return Optional.ofNullable(url);
    }

    public @NotNull Optional<String> hash() {
        return built == null ? Optional.empty() : Optional.of(built.hash());
    }

    public boolean hasPack(@NotNull Player viewer) {
        return LumaGlowAPI.getOkaeriConfig().getEffects().isAssumePackLoaded() || viewer.hasResourcePack();
    }

    public void send(@NotNull Player player) {
        if (!deliverable()) {
            return;
        }
        Config.Effects config = LumaGlowAPI.getOkaeriConfig().getEffects();
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                .id(PACK_ID)
                .uri(URI.create(url))
                .hash(built.hash())
                .build();
        Component prompt = MiniMessage.miniMessage().deserialize(config.getPackPrompt());
        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(config.isPackRequired())
                .prompt(prompt)
                .build());
    }
}
