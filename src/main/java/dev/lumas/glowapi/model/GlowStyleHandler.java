package dev.lumas.glowapi.model;

import dev.lumas.glowapi.LumaGlowAPI;
import dev.lumas.glowapi.pack.PackService;
import dev.lumas.glowapi.effect.GlowEffect;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;
import java.util.function.Consumer;

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
        final ArrayDeque<StyleChange> changes = new ArrayDeque<>();
        volatile @Nullable GlowStyle transientStyle;
        int transientSerial;
        int id = -1;
        int preferredId = 15;
        boolean spatialClear;
        @Nullable UUID markerId;
        int encoded;
        int effectKey;
        boolean spatialSolid;
        int modelCode = -1;
        @Nullable ScheduledTask task;
        boolean initialized;
        int ticks;
    }

    private static final class StyleChange {
        final String description;
        volatile String status = "queued";

        StyleChange(String operation) {
            String caller = StackWalker.getInstance().walk(frames -> frames
                    .filter(frame -> !frame.getClassName().startsWith("dev.lumas.glowapi.model."))
                    .findFirst().map(Object::toString).orElse("unknown caller"));
            description = operation + " from " + caller;
        }
    }

    private void mutate(Entity entity, String operation, Consumer<Live> action) {
        if (entity == null) return;
        Live state = liveFor(entity);
        StyleChange change = new StyleChange(operation);
        synchronized (state.changes) {
            if (state.changes.size() == 6) state.changes.removeFirst();
            state.changes.addLast(change);
        }
        Runnable apply = () -> {
            try {
                if (closed) throw new IllegalStateException("Glow handler has been closed");
                action.accept(state);
                change.status = "applied";
            } catch (RuntimeException | Error failure) {
                change.status = "failed: " + failure.getClass().getSimpleName();
                throw failure;
            }
        };
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            apply.run();
        } else {
            var scheduled = entity.getScheduler().run(LumaGlowAPI.getInstance(), task -> apply.run(),
                    () -> change.status = "entity retired before application");
            if (scheduled == null) change.status = "entity scheduler rejected request";
        }
    }

    private static final int VIEWER_CHECK_TICKS = 20;
    private static final MarkerCleanup MARKER_CLEANUP = new MarkerCleanup(
            Bukkit::getEntity, Bukkit::isOwnedByCurrentRegion,
            () -> LumaGlowAPI.getInstance().isEnabled(),
            marker -> marker.getScheduler().run(LumaGlowAPI.getInstance(), task -> marker.remove(), null));

    private final GlowColorHandler delegate;
    private final Map<UUID, Live> live = new ConcurrentHashMap<>();
    private volatile boolean closed;

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
        mutate(entity, "setStyle(" + style.serialize() + ")", l -> {
            entity.getPersistentDataContainer().set(STYLE_KEY, PersistentDataType.STRING, style.serialize());
            l.transientStyle = null;
            l.transientSerial++;
            sync(entity, l);
        });
    }

    @Override
    public void setTransientStyle(@NotNull Entity entity, @NotNull GlowStyle style, @Nullable Long duration) {
        mutate(entity, "setTransientStyle(" + style.serialize() + ", duration=" + duration + ")", l -> {
            l.transientStyle = style;
            int serial = ++l.transientSerial;
            sync(entity, l);
            if (duration != null) {
                entity.getScheduler().runDelayed(LumaGlowAPI.getInstance(), task -> {
                    if (l.transientSerial == serial) {
                        l.transientStyle = null;
                        sync(entity, l);
                        synchronized (l.changes) {
                            if (l.changes.size() == 6) l.changes.removeFirst();
                            StyleChange expired = new StyleChange("transient expired");
                            expired.status = "applied";
                            l.changes.addLast(expired);
                        }
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
        mutate(entity, "removeColor()", l -> {
            entity.getPersistentDataContainer().remove(STYLE_KEY);
            l.transientStyle = null;
            l.transientSerial++;
            delegate.removeColor(entity);
            sync(entity, l);
        });
    }

    @Override
    public void update(Entity entity) {
        mutate(entity, "update() / clear transient", l -> {
            l.transientStyle = null;
            l.transientSerial++;
            sync(entity, l);
        });
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
        Live l = live.get(entity.getUniqueId());
        if (l != null && l.transientStyle instanceof GlowStyle.Named named) {
            // TAB may read on a different thread or refresh the delegate's cache.
            // The active API override remains authoritative until update/expiry.
            return named.color();
        }
        return delegate.getColor(entity);
    }

    public int effectId(@NotNull Entity entity) {
        Live l = live.get(entity.getUniqueId());
        return l == null ? -1 : l.id;
    }

    public List<String> diagnostics(@NotNull Entity entity) {
        if (!Bukkit.isOwnedByCurrentRegion(entity)) {
            throw new IllegalStateException("Glow diagnostics must run on the entity's region");
        }
        List<String> lines = new ArrayList<>();
        GlowStyle style = getStyle(entity);
        Live l = live.get(entity.getUniqueId());
        ItemDisplay marker = l == null ? null : findMarker(entity, l);
        lines.add("Selected style: " + (style == null ? "none" : style.serialize()));
        lines.add("Active transient override: " + (l == null || l.transientStyle == null
                ? "none" : l.transientStyle.serialize()));
        if (l == null) {
            lines.add("Style requests: none received by this handler");
        } else {
            synchronized (l.changes) {
                if (l.changes.isEmpty()) lines.add("Style requests: none received by this handler");
                for (StyleChange change : l.changes) {
                    lines.add("Style request [" + change.status + "]: " + change.description);
                }
            }
        }
        lines.add("Actual server glowing: " + entity.isGlowing());
        lines.add("Team handler: " + root().getClass().getSimpleName());
        lines.add("Plugin-assigned team color: " + delegate.getColor(entity));
        lines.add("Spatial mode: " + spatialIdentification());
        if (l != null && l.id >= 0) {
            lines.add("Effect identity: " + l.id + " (" + ID_COLORS.get(l.id) + "), payload: "
                    + String.format("%06X", l.encoded & 0xffffff));
            if (spatialIdentification()) {
                lines.add("Spatial allocation clear: " + l.spatialClear);
            }
        }
        lines.add("Marker: " + (marker == null ? "missing" : marker.getUniqueId()));
        if (marker != null) {
            Color color = marker.getGlowColorOverride();
            lines.add("Marker glowing: " + marker.isGlowing() + ", color: "
                    + (color == null ? "none" : String.format("#%06X", color.asRGB())));
            lines.add("Marker visible by default: " + marker.isVisibleByDefault());
        }
        if (!(style instanceof GlowStyle.Effect)) {
            lines.add("Result: this player has an ordinary color, not a custom shader effect.");
        } else if (!entity.isGlowing()) {
            lines.add("Result: no effect marker is emitted while the server glowing flag is off.");
            lines.add("Viewer-only glow from another plugin does not enable this marker.");
        } else if (l == null || l.id < 0 || marker == null) {
            lines.add("Result: custom effect selected but its marker is missing.");
        } else if (spatialIdentification() && !l.spatialClear) {
            lines.add("Result: the server is suppressing this marker because no safe local identity was found.");
        } else if (!marker.isGlowing()) {
            lines.add("Result: the marker is not glowing despite an active effect; inspect its next tick.");
        } else {
            lines.add("Result: effect marker active. This does not verify the client's team color or rendered effect.");
        }
        return List.copyOf(lines);
    }

    private void sync(Entity entity, Live l) {
        if (closed) {
            return;
        }
        if (!l.initialized) {
            MARKER_CLEANUP.removeStalePassengers(entity, MARKER_TAG, l.markerId);
            l.initialized = true;
        }
        boolean isTransient = l.transientStyle != null;
        GlowStyle effective = isTransient ? l.transientStyle : permanentStyle(entity);
        boolean explicit = effective != null;
        if (!explicit) {
            effective = getDefaultStyle(entity);
        }

        if (effective instanceof GlowStyle.Effect e) {
            l.spatialSolid = spatialIdentification() && e.effect().type() == GlowEffect.Type.SOLID;
            l.effectKey = l.spatialSolid ? 0x1000000 | e.effect().rgb() : e.effect().encode(0) & 0xffff;
            if (spatialIdentification()) {
                l.preferredId = ID_COLORS.indexOf(LumaGlowAPI.getOkaeriConfig().getEffects().nametagColor(e));
                updateSpatialIdentity(entity, l, l.effectKey);
            } else if (l.id < 0) {
                l.id = allocateId();
            }
            ensureMarker(entity, l, l.spatialSolid ? e.effect().rgb() : e.effect().encode(l.id));
            delegate.setTransientColor(entity, ID_COLORS.get(l.id), null);
            return;
        }

        dropMarker(l);
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

    private static boolean spatialIdentification() {
        return LumaGlowAPI.getOkaeriConfig().getEffects().isSpatialIdentification();
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
        MARKER_CLEANUP.remove(stray);
    }

    private void ensureMarker(Entity entity, Live l, int encoded) {
        ItemDisplay riding = findMarker(entity, l);
        if (riding != null) {
            if (l.encoded != encoded) {
                riding.setGlowColorOverride(Color.fromRGB(encoded & 0xFFFFFF));
                l.encoded = encoded;
            }
            updateMarkerModel(riding, l);
            mirrorGlow(entity, riding, l);
            return;
        }
        dropStray(l);
        MARKER_CLEANUP.removeStalePassengers(entity, MARKER_TAG, null);
        l.encoded = encoded;
        if (!entity.isValid() || entity.isDead()) {
            return;
        }
        ItemDisplay display = spawnMarker(entity, encoded, l);
        l.markerId = display.getUniqueId();

        if (l.task == null) {
            l.task = entity.getScheduler().runAtFixedRate(LumaGlowAPI.getInstance(),
                    task -> tick(entity, l), () -> live.remove(entity.getUniqueId()), 1L, 1L);
        }
    }

    private void tick(Entity entity, Live l) {
        if (closed || l.id < 0 || live.get(entity.getUniqueId()) != l) {
            dropMarker(l);
            return;
        }
        if (spatialIdentification()) {
            int previousId = l.id;
            updateSpatialIdentity(entity, l, l.effectKey);
            if (l.id != previousId) {
                delegate.setTransientColor(entity, ID_COLORS.get(l.id), null);
            }
        }
        ItemDisplay display = findMarker(entity, l);
        if (display != null) {
            if (spatialIdentification() && !l.spatialSolid) {
                int encoded = ((0x20 | l.id) << 16) | (l.encoded & 0xffff);
                if (encoded != l.encoded) {
                    display.setGlowColorOverride(Color.fromRGB(encoded));
                    l.encoded = encoded;
                }
            }
            updateMarkerModel(display, l);
            mirrorGlow(entity, display, l);
            follow(entity, display);
            if (++l.ticks % VIEWER_CHECK_TICKS == 0) {
                reconcileViewers(display);
            }
        } else {
            if (spatialIdentification() && !l.spatialSolid) {
                l.encoded = ((0x20 | l.id) << 16) | (l.encoded & 0xffff);
            }
            ensureMarker(entity, l, l.encoded);
        }
    }

    private static void mirrorGlow(Entity entity, ItemDisplay marker, Live l) {
        boolean glowing = entity.isGlowing() && (!spatialIdentification() || l.spatialClear);
        if (marker.isGlowing() != glowing) {
            marker.setGlowing(glowing);
        }
    }

    private void updateSpatialIdentity(Entity entity, Live l, int payload) {
        int unavailable = entity.isGlowing() ? SpatialGlowGuard.unavailableColors(entity,
                entity.getNearbyEntities(10.0, 12.0, 10.0), MARKER_TAG,
                Bukkit::isOwnedByCurrentRegion, neighbor -> {
                    Live other = live.get(neighbor.getUniqueId());
                    if (other != null && other.id >= 0) {
                        return other.effectKey == payload ? 0 : 1 << other.id;
                    }
                    return SpatialGlowGuard.ordinaryColorMask(ID_COLORS, delegate.getColor(neighbor));
                }) : 0;
        int selected = SpatialGlowGuard.selectColor(l.id, l.preferredId, unavailable);
        l.spatialClear = selected >= 0;
        l.id = selected >= 0 ? selected : l.id >= 0 ? l.id : l.preferredId;
    }

    private static ItemStack markerStack(Live l) {
        l.modelCode = spatialIdentification() ? l.id + (l.spatialSolid ? 16 : 0) : -1;
        ItemStack stack = ItemStack.of(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, l.modelCode < 0 ? MARKER_MODEL
                : Key.key("lumaglowapi", "marker_" + l.modelCode));
        return stack;
    }

    private static void updateMarkerModel(ItemDisplay marker, Live l) {
        int wanted = spatialIdentification() ? l.id + (l.spatialSolid ? 16 : 0) : -1;
        if (l.modelCode != wanted) {
            marker.setItemStack(markerStack(l));
        }
    }

    private ItemDisplay spawnMarker(Entity entity, int encoded, Live l) {
        ItemStack stack = markerStack(l);

        boolean assumePackLoaded = LumaGlowAPI.getOkaeriConfig().getEffects().isAssumePackLoaded();
        Transformation transform = markerTransform(entity);
        ItemDisplay display = entity.getWorld().spawn(entity.getLocation(), ItemDisplay.class, d -> {
            if (spatialIdentification()) {
                d.setRotation(0f, 0f);
            }
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
            d.setGlowing(entity.isGlowing() && (!spatialIdentification() || l.spatialClear));

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
        if (spatialIdentification()) {
            return new Transformation(new Vector3f(0f, -(float) entity.getHeight() + 1.0f, 0f),
                    new AxisAngle4f(), new Vector3f(2.0f), new AxisAngle4f());
        }
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

    private void dropMarker(Live l) {
        if (l.task != null) {
            l.task.cancel();
            l.task = null;
        }
        dropStray(l);
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
        // Joining initializes the display without cancelling an override that
        // another plugin already supplied in its PlayerJoinEvent listener.
        runOwned(player, () -> sync(player, liveFor(player)));
    }

    @ApiStatus.Internal
    @Override
    public void removePlayer(Player player) {
        Live l = live.remove(player.getUniqueId());
        if (l != null) {
            dropMarker(l);
        }
        delegate.removePlayer(player);
    }

    @ApiStatus.Internal
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (Live l : live.values()) {
            dropMarker(l);
        }
        live.clear();
        delegate.close();
    }
}
