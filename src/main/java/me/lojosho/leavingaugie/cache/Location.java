package me.lojosho.leavingaugie.cache;

import org.springframework.lang.NonNull;

public class Location {

    private final int id;
    private final String name;
    private final boolean origin;

    public Location(int id, String name, boolean origin) {
        this.id = id;
        this.name = name;
        this.origin = origin;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public boolean isOrigin() {
        return origin;
    }

    public boolean isDestination() {
        return !origin;
    }

    @Override
    public String toString() {
        return name;
    }
}
