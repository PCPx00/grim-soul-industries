package com.xtri6.grimsoul.machine;

/** What one side of a machine does with items. */
public enum ItemSideMode {
    OFF("off"), INPUT("in"), OUTPUT("out"), BOTH("both");

    private final String id;

    ItemSideMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean canInput() {
        return this == INPUT || this == BOTH;
    }

    public boolean canOutput() {
        return this == OUTPUT || this == BOTH;
    }

    public ItemSideMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static ItemSideMode byIndex(int index) {
        ItemSideMode[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
