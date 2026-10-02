package com.xtri6.grimsoul.machine;

/** How a machine reacts to redstone. */
public enum RedstoneMode {
    /** Runs whether or not it gets a signal. */
    IGNORE("ignore"),
    /** Runs only while powered by redstone. */
    HIGH("high"),
    /** Stops while powered by redstone. */
    LOW("low");

    private final String id;

    RedstoneMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneMode byIndex(int index) {
        RedstoneMode[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
