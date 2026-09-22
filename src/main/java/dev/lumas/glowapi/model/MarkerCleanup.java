package dev.lumas.glowapi.model;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

final class MarkerCleanup {
    private final Function<UUID, Entity> lookup;
    private final Predicate<Entity> owned;
    private final BooleanSupplier schedulingAllowed;
    private final Consumer<Entity> scheduleRemoval;

    MarkerCleanup(Function<UUID, Entity> lookup, Predicate<Entity> owned,
                  BooleanSupplier schedulingAllowed, Consumer<Entity> scheduleRemoval) {
        this.lookup = lookup;
        this.owned = owned;
        this.schedulingAllowed = schedulingAllowed;
        this.scheduleRemoval = scheduleRemoval;
    }

    void remove(@Nullable UUID markerId) {
        if (markerId == null) {
            return;
        }
        Entity marker = lookup.apply(markerId);
        if (marker == null) {
            return;
        }
        if (owned.test(marker)) {
            marker.remove();
        } else if (schedulingAllowed.getAsBoolean()) {
            scheduleRemoval.accept(marker);
        }
    }

    void removeStalePassengers(Entity owner, String markerTag, @Nullable UUID currentMarker) {
        for (Entity passenger : owner.getPassengers()) {
            if (owned.test(passenger) && passenger.getScoreboardTags().contains(markerTag)
                    && !passenger.getUniqueId().equals(currentMarker)) {
                passenger.remove();
            }
        }
    }
}
