package dev.lumas.glowapi.model;

import org.bukkit.entity.Entity;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class SpatialGlowGuard {
    private static final int[] COLOR_PREFERENCE = {15, 7, 8, 14, 11, 13, 10, 6, 12, 9, 3, 2, 5, 4, 1, 0};

    private SpatialGlowGuard() {
    }

    public static int ordinaryColorMask(List<NamedTextColor> teamColors, @Nullable TextColor color) {
        int id = color == null ? -1 : teamColors.indexOf(color);
        return 1 << (id < 0 ? 15 : id);
    }

    public static int unavailableColors(Entity owner, Collection<Entity> neighbors, String markerTag,
                                        Predicate<Entity> isOwned, ToIntFunction<Entity> conflictingColors) {
        int unavailable = 0;
        for (Entity neighbor : neighbors) {
            if (!isOwned.test(neighbor)) {
                return 0xffff;
            }
            if (neighbor.getScoreboardTags().contains(markerTag) || neighbor.getVehicle() == owner) {
                continue;
            }
            if (!neighbor.isGlowing()) {
                continue;
            }
            unavailable |= conflictingColors.applyAsInt(neighbor);
        }
        return unavailable;
    }

    public static int selectColor(int current, int preferred, int unavailable) {
        // TAB polls placeholders independently of marker metadata. Keep a safe
        // identity instead of chasing the preferred color as neighbors move.
        if (current >= 0 && (unavailable & (1 << current)) == 0) {
            return current;
        }
        return selectColor(preferred, unavailable);
    }

    public static int selectColor(int preferred, int unavailable) {
        if ((unavailable & (1 << preferred)) == 0) {
            return preferred;
        }
        for (int color : COLOR_PREFERENCE) {
            if ((unavailable & (1 << color)) == 0) {
                return color;
            }
        }
        return -1;
    }
}
