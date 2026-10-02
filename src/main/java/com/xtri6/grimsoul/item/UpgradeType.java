package com.xtri6.grimsoul.item;

/**
 * Every upgrade card in the mod. The max count is how many of that card one machine accepts.
 * Cards themselves stack to 64 in a player's inventory.
 */
public enum UpgradeType {
    SPEED("speed", 30),
    RANGE("range", 10),
    DAMAGE("damage", 35),
    LOOTING("looting", 10),
    PLAYER_KILL("player_kill", 1),
    STORAGE("storage", 5),
    VOID("void", 1),
    FIRE_ASPECT("fire_aspect", 10),
    SMITE("smite", 10),
    BANE_OF_ARTHROPODS("bane_of_arthropods", 10),
    // Essence Spawner cards
    COUNT("count", 19),
    NO_PLAYER_NEEDED("no_player_needed", 1),
    IGNORE_LIGHT("ignore_light", 1),
    IGNORE_SPAWN_CONDITIONS("ignore_spawn_conditions", 1),
    // Simulation Chamber card
    EFFICIENCY("efficiency", 20);

    private final String id;
    private final int maxCards;

    UpgradeType(String id, int maxCards) {
        this.id = id;
        this.maxCards = maxCards;
    }

    public String id() {
        return id;
    }

    public int maxCards() {
        return maxCards;
    }

    /** Registry name of the card item, e.g. "speed_card". */
    public String itemName() {
        return id + "_card";
    }
}
