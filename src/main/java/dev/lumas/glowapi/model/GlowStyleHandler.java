package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.pack.PackService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowStyleHandler implements GlowColorHandler {

    public static final NamespacedKey STYLE_KEY = new NamespacedKey(LumaGlowAPI.getInstance(), "style");

    public static final List<NamedTextColor> ID_COLORS = List.of(
            NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN,
            NamedTextColor.DARK_AQUA, NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE,
            NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY,
            NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
            NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW,
            NamedTextColor.WHITE);

    private static final int[] ID_PREFERENCE = {15, 7, 8, 5, 1, 2, 3, 4, 6, 9, 10, 11, 13, 14, 12, 0};

    public static final Key MARKER_MODEL = Key.key("lumaglowapi", "marker");
    public static final String MARKER_TAG = "lumaglowapi_marker";

    private static final class Live {
        @Nullable GlowStyle transientStyle;
        int transientSerial;
        int id = -1;
        @Nullable UUID markerId;
        int encoded;
        @Nullable ScheduledTask task;
        int ticks;
    }

    private static final int VIEWER_CHECK_TICKS = 20;

    private final GlowColorHandler delegate;
    private final Map<UUID, Live> live = new ConcurrentHashMap<>();

    public GlowStyleHandler(@NotNull GlowColorHandler delegate) {
        if (delegate instanceof GlowStyleHandler) {
            throw new IllegalArgumentException("GlowStyleHandler cannot wrap itself");
        }
        this.delegate = delegate;
    }

    public @NotNull GlowColorHandler delegate() {
        return delegate;
    }

    @Override
    public @NotNull GlowColorHandler root() {
        return delegate.root();
    }

    @Override
    public void setStyle(@NotNull Entity entity, @NotNull GlowStyle style) {
        runOwned(entity, () -> {
            entity.getPersistentDataContainer().set(STYLE_KEY, PersistentDataType.STRING, style.serialize());
            Live l = liveFor(entity);
            l.transientStyle = null;
            l.transientSerial++;
            sync(entity, l);
        });
    }

    @Override
    public void setTransientStyle(@NotNull Entity entity, @NotNull GlowStyle style, @Nullable Long duration) {
        runOwned(entity, () -> {
            Live l = liveFor(entity);
            l.transientStyle = style;
            int serial = ++l.transientSerial;
            sync(entity, l);
            if (duration != null) {
                entity.getScheduler().runDelayed(LumaGlowAPI.getInstance(), task -> {
                    if (l.transientSerial == serial) {
                        l.transientStyle = null;
                        sync(entity, l);
                    }
                }, null, Math.max(1L, duration));
            }
        });
    }

    @Override
    public void setColor(Entity entity, NamedTextColor color) {
        setStyle(entity, GlowStyle.of(color));
    }

    @Override
    public void setTransientColor(Entity entity, NamedTextColor color, @Nullable Long duration) {
        setTransientStyle(entity, GlowStyle.of(color), duration);
    }

    @Override
    public void removeColor(Entity entity) {
        runOwned(entity, () -> {
            entity.getPersistentDataContainer().remove(STYLE_KEY);
            Live l = liveFor(entity);
            l.transientStyle = null;
            l.transientSerial++;
            delegate.removeColor(entity);
            sync(entity, l);
        });
    }

    @Override
    public void update(Entity entity) {
        runOwned(entity, () -> sync(entity, liveFor(entity)));
    }

    @Override
    public @Nullable GlowStyle getStyle(@NotNull Entity entity) {
        Live l = live.get(entity.getUniqueId());
        if (l != null && l.transientStyle != null) {
            return l.transientStyle;
        }
        GlowStyle permanent = permanentStyle(entity);
        return permanent != null ? permanent : getDefaultStyle(entity);
    }

    @Override
    public @Nullable TextColor getColor(Entity entity) {
        return delegate.getColor(entity);
    }

    public int effectId(@NotNull Entity entity) {
        Live l = live.get(entity.getUniqueId());
        return l == null ? -1 : l.id;
    }

    private void sync(Entity entity, Live l) {
        boolean isTransient = l.transientStyle != null;
        GlowStyle effective = isTransient ? l.transientStyle : permanentStyle(entity);
        boolean explicit = effective != null;
        if (!explicit) {
            effective = getDefaultStyle(entity);
        }

        if (effective instanceof GlowStyle.Effect e) {
            if (l.id < 0) {
                l.id = allocateId();
            }
            ensureMarker(entity, l, e.effect().encode(l.id));
            delegate.setTransientColor(entity, ID_COLORS.get(l.id), null);
            return;
        }

        dropMarker(entity, l);
        l.id = -1;
        if (effective instanceof GlowStyle.Named(NamedTextColor color) && explicit) {
            if (isTransient) {
                delegate.setTransientColor(entity, color, null);
            } else {
                delegate.setColor(entity, color);
            }
        } else {
            delegate.update(entity);
        }
    }

    private @Nullable GlowStyle permanentStyle(Entity entity) {
        String stored = entity.getPersistentDataContainer().get(STYLE_KEY, PersistentDataType.STRING);
        if (stored != null) {
            GlowStyle style = GlowStyle.parse(stored);
            if (style != null) {
                return style;
            }
        }

        String legacy = entity.getPersistentDataContainer().get(COLOR_KEY, PersistentDataType.STRING);
        if (legacy != null) {
            NamedTextColor color = NamedTextColor.NAMES.value(legacy);
            if (color != null) {
                return GlowStyle.of(color);
            }
        }
        return null;
    }

    private Live liveFor(Entity entity) {
        return live.computeIfAbsent(entity.getUniqueId(), k -> new Live());
    }

    private int allocateId() {
        int[] users = new int[ID_COLORS.size()];
        for (Live l : live.values()) {
            if (l.id >= 0) {
                users[l.id]++;
            }
        }
        Collection<GlowStyle> defaults = LumaGlowAPI.getOkaeriConfig().getDefaultColors().values();
        int best = -1;
        for (int pass = 0; pass < 2 && best < 0; pass++) {
            for (int id : ID_PREFERENCE) {
                boolean reserved = defaults.contains(GlowStyle.of(ID_COLORS.get(id)));
                if (pass == 0 && reserved) {
                    continue;
                }
                if (best < 0 || users[id] < users[best]) {
                    best = id;
                }
            }
        }
        return best;
    }

    private static @Nullable ItemDisplay findMarker(Entity entity, Live l) {
        if (l.markerId == null) {
            return null;
        }
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof ItemDisplay display && l.markerId.equals(passenger.getUniqueId())) {
                return display.isValid() ? display : null;
            }
        }
        return null;
    }

    private static void dropStray(Live l) {
        UUID stray = l.markerId;
        l.markerId = null;
        if (stray != null) {
            Entity entity = Bukkit.getEntity(stray);
            if (entity != null) {
                runOwned(entity, entity::remove);
            }
        }
    }

    private void ensureMarker(Entity entity, Live l, int encoded) {
        ItemDisplay riding = findMarker(entity, l);
        if (riding != null) {
            if (l.encoded != encoded) {
                riding.setGlowColorOverride(Color.fromRGB(encoded & 0xFFFFFF));
                l.encoded = encoded;
            }
            mirrorGlow(entity, riding);
            return;
        }
        dropStray(l);
        l.encoded = encoded;
        if (!entity.isValid() || entity.isDead()) {
            return;
        }
        ItemDisplay display = spawnMarker(entity, encoded);
        l.markerId = display.getUniqueId();

        if (l.task == null) {
            l.task = entity.getScheduler().runAtFixedRate(LumaGlowAPI.getInstance(),
                    task -> tick(entity, l), () -> live.remove(entity.getUniqueId()), 1L, 1L);
        }
    }

    private void tick(Entity entity, Live l) {
        if (l.id < 0 || !live.containsKey(entity.getUniqueId())) {
            dropMarker(entity, l);
            return;
        }
        ItemDisplay display = findMarker(entity, l);
        if (display != null) {

            mirrorGlow(entity, display);
            follow(entity, display);
            if (++l.ticks % VIEWER_CHECK_TICKS == 0) {
                reconcileViewers(display);
            }
        } else {
            ensureMarker(entity, l, l.encoded);
        }
    }

    private static void mirrorGlow(Entity entity, ItemDisplay marker) {
        boolean glowing = entity.isGlowing();
        if (marker.isGlowing() != glowing) {
            marker.setGlowing(glowing);
        }
    }

    private ItemDisplay spawnMarker(Entity entity, int encoded) {
        ItemStack stack = ItemStack.of(Material.PAPER);

        stack.setData(DataComponentTypes.ITEM_MODEL, MARKER_MODEL);

        boolean assumePackLoaded = LumaGlowAPI.getOkaeriConfig().getEffects().isAssumePackLoaded();
        Transformation transform = markerTransform(entity);
        ItemDisplay display = entity.getWorld().spawn(entity.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(stack);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(transform);
            d.setTeleportDuration(1);
            d.setInterpolationDuration(0);
            d.setViewRange(1.0f);

            d.setDisplayWidth(0f);
            d.setDisplayHeight(0f);
            d.setShadowRadius(0f);
            d.setPersistent(false);
            d.setSilent(true);
            d.addScoreboardTag(MARKER_TAG);
            d.setGlowColorOverride(Color.fromRGB(encoded & 0xFFFFFF));
            d.setGlowing(entity.isGlowing());

            d.setVisibleByDefault(assumePackLoaded);
        });

        entity.addPassenger(display);

        if (!assumePackLoaded) {
            reconcileViewers(display);
        }
        return display;
    }

    private static void reconcileViewers(ItemDisplay marker) {
        PackService pack = PackService.getInstance();
        boolean visibleByDefault = marker.isVisibleByDefault();
        for (Player viewer : Bukkit.getOnlinePlayers()) {

            if (!Bukkit.isOwnedByCurrentRegion(viewer)) {
                continue;
            }
            boolean should = pack.hasPack(viewer);
            boolean sees = viewer.canSee(marker);
            if (should && !sees) {
                viewer.showEntity(LumaGlowAPI.getInstance(), marker);
            } else if (!should && sees && !visibleByDefault) {
                viewer.hideEntity(LumaGlowAPI.getInstance(), marker);
            }
        }
    }

    private static Transformation markerTransform(Entity entity) {
        float gap = (float) LumaGlowAPI.getOkaeriConfig().getEffects().getMarkerGap();
        float scale = Math.max(1.0f, (float) entity.getWidth() + 0.4f);

        return new Transformation(
                new Vector3f(0f, -(float) entity.getHeight() + gap, 0f),
                new AxisAngle4f(),
                new Vector3f(scale),
                new AxisAngle4f());
    }

    private static void follow(Entity entity, ItemDisplay marker) {
        Transformation wanted = markerTransform(entity);
        Transformation current = marker.getTransformation();
        if (Math.abs(current.getTranslation().y - wanted.getTranslation().y) > 0.01f
                || Math.abs(current.getScale().x - wanted.getScale().x) > 0.01f) {
            marker.setTransformation(wanted);
        }
    }

    private void dropMarker(Entity entity, Live l) {
        if (l.task != null) {
            l.task.cancel();
            l.task = null;
        }
        for (Entity passenger : entity.getPassengers()) {
            runOwned(passenger, () -> {
                if (passenger.getScoreboardTags().contains(MARKER_TAG)) {
                    passenger.remove();
                }
            });
        }
        if (l.markerId != null) {
            Entity stray = Bukkit.getEntity(l.markerId);
            if (stray != null) {
                runOwned(stray, stray::remove);
            }
            l.markerId = null;
        }
    }

    private static void runOwned(@Nullable Entity entity, Runnable task) {
        if (entity == null) {
            return;
        }
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
        } else {
            entity.getScheduler().run(LumaGlowAPI.getInstance(), t -> task.run(), null);
        }
    }

    @ApiStatus.Internal
    @Override
    public void addPlayer(Player player) {
        delegate.addPlayer(player);
    }

    @ApiStatus.Internal
    @Override
    public void removePlayer(Player player) {
        Live l = live.remove(player.getUniqueId());
        if (l != null) {
            runOwned(player, () -> dropMarker(player, l));
        }
        delegate.removePlayer(player);
    }

    @ApiStatus.Internal
    @Override
    public void close() {
        for (Map.Entry<UUID, Live> entry : live.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null) {
                Live l = entry.getValue();
                entity.getScheduler().execute(LumaGlowAPI.getInstance(), () -> {
                    dropMarker(entity, l);
                }, null, 1);
            }
        }
        live.clear();
        delegate.close();
    }
}
