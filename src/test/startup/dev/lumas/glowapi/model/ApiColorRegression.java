package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.effect.GlowEffect;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ApiColorRegression {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void field(Class<?> type, Object target, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    public static void run() throws Exception {
        // Only supply the Bukkit services used by the real handlers; do not start
        // a server or invoke JavaPlugin's classloader-dependent constructor.
        var unsafeType = Class.forName("sun.misc.Unsafe");
        var unsafeField = unsafeType.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object plugin = unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(unsafeField.get(null), LumaGlowAPI.class);
        var description = new PluginDescriptionFile("LumaGlowAPI", "test", LumaGlowAPI.class.getName());
        field(JavaPlugin.class, plugin, "description", description);
        field(JavaPlugin.class, plugin, "pluginMeta", description);
        field(LumaGlowAPI.class, null, "instance", plugin);
        boolean[] owned = {true};
        field(Bukkit.class, null, "server", proxy(Server.class, (p, method, args) -> {
            if (method.getName().equals("isOwnedByCurrentRegion")) return owned[0];
            throw new AssertionError("Unexpected server call: " + method.getName());
        }));

        var defaults = LumaGlowAPI.getOkaeriConfig().getDefaultColors();
        var savedDefaults = new HashMap<>(defaults);
        try {
            for (GlowStyle fallback : List.of(GlowStyle.of(NamedTextColor.RED), GlowStyle.of(GlowEffect.rainbow()))) {
                defaults.clear();
                defaults.put("test.default", fallback);
                Map<NamespacedKey, Object> data = new HashMap<>();
                List<Consumer<ScheduledTask>> expiry = new ArrayList<>();
                List<Consumer<ScheduledTask>> queued = new ArrayList<>();
                var pdc = proxy(PersistentDataContainer.class, (p, method, args) -> switch (method.getName()) {
                    case "get" -> data.get(args[0]);
                    case "set" -> { data.put((NamespacedKey) args[0], args[2]); yield null; }
                    case "remove" -> { data.remove(args[0]); yield null; }
                    default -> throw new AssertionError("Unexpected PDC call: " + method.getName());
                });
                var scheduler = proxy(EntityScheduler.class, (p, method, args) -> {
                    if (method.getName().equals("run")) {
                        queued.add((Consumer<ScheduledTask>) args[1]);
                        return proxy(ScheduledTask.class, (t, m, a) -> null);
                    }
                    if (method.getName().equals("runDelayed")) {
                        expiry.add((Consumer<ScheduledTask>) args[1]);
                        return null;
                    }
                    throw new AssertionError("Unexpected scheduler call: " + method.getName());
                });
                boolean[] glowing = {true};
                UUID id = UUID.randomUUID();
                Player player = proxy(Player.class, (p, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getPersistentDataContainer" -> pdc;
                    case "getPassengers", "getNearbyEntities" -> List.of();
                    case "getScheduler" -> scheduler;
                    // API calls must not require command or color permissions.
                    case "hasPermission" -> args[0].equals("test.default");
                    case "isGlowing" -> glowing[0];
                    case "isValid" -> false;
                    default -> throw new AssertionError("Unexpected player call: " + method.getName());
                });
                var tab = new PlaceHolderTeamHandler(new UnusedEntityHandler());
                var handler = new GlowStyleHandler(tab);
                var api = new GlowColorManager(handler);
                field(GlowColorManager.class, null, "instance", api);
                api.update(player);
                require(api.getStyle(player).equals(fallback), "Initial default was not selected");
                api.setTransientColor(player, NamedTextColor.BLACK);
                require(api.getStyle(player).equals(GlowStyle.of(NamedTextColor.BLACK)), "Default overrides API style");
                require(api.getColor(player) == NamedTextColor.BLACK && tab.getColor(player) == NamedTextColor.BLACK,
                        "TAB does not receive the API's black color");
                require("\u00a70".equals(new dev.lumas.glowapi.papi.ColorPlaceholder()
                        .onPlaceholderRequest(player, "color")), "Real TAB placeholder did not emit black while glowing");
                require(handler.effectId(player) == -1, "Default shader identity survived a named API override");
                require(data.isEmpty(), "Transient override was persisted");
                require(handler.diagnostics(player).stream().anyMatch(line ->
                                line.contains("Style request [applied]: setTransientStyle(BLACK")),
                        "Diagnostics did not record the API request");
                api.addPlayer(player);
                require(api.getColor(player) == NamedTextColor.BLACK && tab.getColor(player) == NamedTextColor.BLACK,
                        "Join initialization replaced an earlier API override");
                tab.update(player);
                require(api.getColor(player) == NamedTextColor.BLACK,
                        "A delegate cache refresh hid the active API override from TAB");
                api.update(player);
                require(api.getStyle(player).equals(fallback), "update() did not release the API override");
                require(api.getColor(player) != NamedTextColor.BLACK, "TAB retained black after update()");

                // This is the same style entry point used by the command's -transient flag.
                api.setTransientStyle(player, GlowStyle.of(NamedTextColor.BLACK));
                require(api.getColor(player) == NamedTextColor.BLACK, "Command style path differs from color API");
                api.update(player);
                owned[0] = false;
                api.setTransientColor(player, NamedTextColor.BLACK);
                owned[0] = true;
                require(handler.diagnostics(player).stream().anyMatch(line ->
                                line.contains("Style request [queued]: setTransientStyle(BLACK")),
                        "Diagnostics did not expose a pending API request");
                queued.removeFirst().accept(null);
                require(api.getStyle(player).equals(GlowStyle.of(NamedTextColor.BLACK))
                                && api.getColor(player) == NamedTextColor.BLACK,
                        "An external-thread API request did not override the default");
                api.update(player);
                List<String> diagnostics = handler.diagnostics(player);
                require(diagnostics.contains("Active transient override: none")
                                && diagnostics.stream().anyMatch(line -> line.contains("update() / clear transient")),
                        "Diagnostics did not expose the call that cleared black");

                api.setColor(player, NamedTextColor.BLUE);
                api.setTransientColor(player, NamedTextColor.BLACK, 10L);
                api.update(player);
                require(api.getColor(player) == NamedTextColor.BLUE, "update() did not restore the saved color");
                api.setTransientColor(player, NamedTextColor.GREEN);
                expiry.removeFirst().accept(null);
                require(api.getColor(player) == NamedTextColor.GREEN, "An old expiry cleared a newer override");
                api.setTransientColor(player, NamedTextColor.BLACK, 10L);
                expiry.removeFirst().accept(null);
                require(api.getColor(player) == NamedTextColor.BLUE, "Expiry did not restore the saved color");
                api.removeColor(player);
                require(api.getStyle(player).equals(fallback), "Removing saved color did not restore default");
                api.close();
            }
        } finally {
            defaults.clear();
            defaults.putAll(savedDefaults);
            field(GlowColorManager.class, null, "instance", null);
            field(Bukkit.class, null, "server", null);
            field(LumaGlowAPI.class, null, "instance", null);
        }
        System.out.println("API colors override named/effect defaults through TAB; update and expiry restore the saved/default style");
    }

    private static final class UnusedEntityHandler implements GlowColorHandler {
        public void setColor(org.bukkit.entity.Entity e, NamedTextColor c) { throw new AssertionError(); }
        public void setTransientColor(org.bukkit.entity.Entity e, NamedTextColor c, Long d) { throw new AssertionError(); }
        public void removeColor(org.bukkit.entity.Entity e) { throw new AssertionError(); }
        public void update(org.bukkit.entity.Entity e) { throw new AssertionError(); }
        public net.kyori.adventure.text.format.TextColor getColor(org.bukkit.entity.Entity e) { throw new AssertionError(); }
    }
}
