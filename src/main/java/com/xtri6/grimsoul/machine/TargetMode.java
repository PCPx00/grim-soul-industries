package com.xtri6.grimsoul.machine;

/** Which entities the Reaper Block attacks. */
public enum TargetMode {
    ALL("all"),
    PLAYERS("players"),
    FRIENDLY("friendly"),
    HOSTILE("hostile");

    private final String id;

    TargetMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public TargetMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static TargetMode byIndex(int index) {
        TargetMode[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
