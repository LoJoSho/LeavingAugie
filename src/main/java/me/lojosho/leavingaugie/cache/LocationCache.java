package me.lojosho.leavingaugie.cache;

import me.lojosho.leavingaugie.LeavingAugieApplication;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class LocationCache {

    private static final ConcurrentHashMap<Integer, Location> LOCATIONS = new ConcurrentHashMap<>();

    public static void refreshCache() {
        LOCATIONS.clear();

        try {
            for (Location location : LeavingAugieApplication.getDatabase().getLocations()) {
                LOCATIONS.put(location.getId(), location);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static @Nullable Location getLocation(int id) {
        return LOCATIONS.get(id);
    }

    public static @NonNull Collection<Location> getLocations() {
        return LOCATIONS.values();
    }
}
