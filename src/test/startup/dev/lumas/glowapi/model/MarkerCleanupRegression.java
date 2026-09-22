package dev.lumas.glowapi.model;

import org.bukkit.entity.Entity;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MarkerCleanupRegression {
    private static final class TestEntity {
        final UUID id = UUID.randomUUID();
        final List<Entity> passengers = new ArrayList<>();
        final boolean tagged;
        boolean owned = true;
        boolean removed;
        final Entity entity;

        TestEntity(boolean tagged) {
            this.tagged = tagged;
            this.entity = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getUniqueId" -> id;
                        case "getPassengers" -> List.copyOf(passengers);
                        case "getScoreboardTags" -> {
                            require(owned, "Marker tags read from another region");
                            yield tagged ? Set.of("marker") : Set.of();
                        }
                        case "remove" -> {
                            require(owned, "Marker removed from another region");
                            removed = true;
                            yield null;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        default -> throw new AssertionError("Unexpected entity access: " + method.getName());
                    });
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void run() {
        Map<UUID, TestEntity> entities = new HashMap<>();
        List<Runnable> queued = new ArrayList<>();
        boolean[] enabled = {true};
        MarkerCleanup cleanup = new MarkerCleanup(
                id -> entities.containsKey(id) ? entities.get(id).entity : null,
                entity -> entities.get(entity.getUniqueId()).owned,
                () -> enabled[0], entity -> queued.add(() -> entity.remove()));
        TestEntity owner = new TestEntity(false);
        TestEntity old = new TestEntity(true);
        TestEntity replacement = new TestEntity(true);
        TestEntity cosmetic = new TestEntity(false);
        for (TestEntity entity : List.of(owner, old, replacement, cosmetic)) {
            entities.put(entity.id, entity);
        }
        owner.passengers.add(old.entity);
        old.owned = false;
        cleanup.remove(old.id);
        require(queued.size() == 1 && !old.removed, "Remote marker cleanup was not deferred");
        owner.passengers.add(replacement.entity);
        owner.passengers.add(cosmetic.entity);
        old.owned = true;
        queued.removeFirst().run();
        require(old.removed && !replacement.removed && !cosmetic.removed,
                "Old cleanup deleted a replacement marker or cosmetic");

        old.removed = false;
        cleanup.removeStalePassengers(owner.entity, "marker", replacement.id);
        require(old.removed && !replacement.removed && !cosmetic.removed,
                "Stale passenger sweep did not preserve the active marker and cosmetics");

        old.removed = false;
        old.owned = false;
        enabled[0] = false;
        cleanup.remove(old.id);
        cleanup.removeStalePassengers(owner.entity, "marker", replacement.id);
        require(queued.isEmpty() && !old.removed,
                "Disabled plugin scheduled cleanup or touched a foreign-region marker");
        old.owned = true;
        cleanup.removeStalePassengers(owner.entity, "marker", null);
        require(old.removed && replacement.removed && !cosmetic.removed,
                "A fresh handler did not clear PlugMan's stale marker passengers");
        cleanup.remove(null);
        cleanup.remove(UUID.randomUUID());
        System.out.println("Marker reload cleanup preserves successors, removes stale passengers and avoids disabled/cross-region work");
    }
}
