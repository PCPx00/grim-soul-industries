package com.xtri6.grimsoul;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side balance settings. Pack devs can edit these in config/grimsoul-common.toml.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- Reaper Block ----
    public static final ModConfigSpec.IntValue REAPER_BASE_INTERVAL = BUILDER
            .comment("Ticks between Reaper Block attack cycles with no Speed cards (20 ticks = 1 second).")
            .defineInRange("reaper.baseInterval", 20, 2, 200);
    public static final ModConfigSpec.IntValue REAPER_MIN_INTERVAL = BUILDER
            .comment("Fastest attack interval in ticks, reached with a full stack of Speed cards.")
            .defineInRange("reaper.minInterval", 4, 1, 200);
    public static final ModConfigSpec.DoubleValue REAPER_BASE_DAMAGE = BUILDER
            .comment("Damage per hit with no Damage cards.")
            .defineInRange("reaper.baseDamage", 4.0, 0.0, 10000.0);
    public static final ModConfigSpec.DoubleValue REAPER_DAMAGE_PER_CARD = BUILDER
            .comment("Extra damage per hit for each Damage card.")
            .defineInRange("reaper.damagePerCard", 2.0, 0.0, 10000.0);
    public static final ModConfigSpec.IntValue REAPER_COLLECT_INTERVAL = BUILDER
            .comment("Ticks between item and XP collection sweeps.")
            .defineInRange("reaper.collectInterval", 10, 1, 200);
    public static final ModConfigSpec.BooleanValue REAPER_KILL_NAMED = BUILDER
            .comment("If false, mobs with a name tag are never attacked. Players are not affected by this.")
            .define("reaper.killNamedMobs", false);
    public static final ModConfigSpec.BooleanValue REAPER_KILL_PETS = BUILDER
            .comment("If false, tamed pets (wolves, cats, horses...) are never attacked.")
            .define("reaper.killTamedPets", false);

    // ---- Mob Fan ----
    public static final ModConfigSpec.IntValue FAN_BASE_RANGE = BUILDER
            .comment("How many blocks in front of the Mob Fan it pushes, with no Range cards.")
            .defineInRange("fan.baseRange", 4, 1, 32);
    public static final ModConfigSpec.IntValue FAN_RANGE_PER_CARD = BUILDER
            .comment("Extra blocks of push range per Range card.")
            .defineInRange("fan.rangePerCard", 2, 0, 16);
    public static final ModConfigSpec.DoubleValue FAN_PUSH_STRENGTH = BUILDER
            .comment("How hard the Mob Fan pushes each tick (speed added per tick, in blocks).")
            .defineInRange("fan.pushForce", 0.3, 0.0, 4.0);
    public static final ModConfigSpec.DoubleValue FAN_MAX_SPEED = BUILDER
            .comment("Fastest the Mob Fan can push something, in blocks per tick (1.0 = 20 blocks per second).")
            .defineInRange("fan.maxSpeed", 1.5, 0.1, 10.0);

    // ---- XP Tank ----
    public static final ModConfigSpec.IntValue TANK_CAPACITY = BUILDER
            .comment("Maximum XP points one XP Tank can hold.")
            .defineInRange("xpTank.capacity", 1_000_000, 100, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue TANK_BASE_RADIUS = BUILDER
            .comment("Radius in blocks the XP Tank pulls XP orbs from, with no Range cards.")
            .defineInRange("xpTank.baseRadius", 3, 1, 32);
    public static final ModConfigSpec.IntValue TANK_RADIUS_PER_CARD = BUILDER
            .comment("Extra radius per Range card.")
            .defineInRange("xpTank.radiusPerCard", 2, 0, 16);

    // ---- Essence Spawner ----
    public static final ModConfigSpec.IntValue SPAWNER_BASE_INTERVAL = BUILDER
            .comment("Ticks between Essence Spawner spawns with no Speed cards (20 ticks = 1 second).")
            .defineInRange("spawner.baseInterval", 200, 10, 6000);
    public static final ModConfigSpec.IntValue SPAWNER_MIN_INTERVAL = BUILDER
            .comment("Fastest spawn interval in ticks, reached with a full stack of Speed cards.")
            .defineInRange("spawner.minInterval", 10, 1, 6000);
    public static final ModConfigSpec.IntValue SPAWNER_BASE_RADIUS = BUILDER
            .comment("How far from the Essence Spawner mobs can appear (horizontal radius), with no Range cards.")
            .defineInRange("spawner.baseRadius", 4, 1, 32);
    public static final ModConfigSpec.IntValue SPAWNER_ENTITY_CAP = BUILDER
            .comment("The spawner pauses while this many of its mob are already around it (keeps TPS healthy).")
            .defineInRange("spawner.entityCap", 30, 1, 1000);
    public static final ModConfigSpec.IntValue SPAWNER_PLAYER_RANGE = BUILDER
            .comment("A player must be this close for the spawner to run, unless it has a No Player Needed card.")
            .defineInRange("spawner.playerRange", 16, 1, 128);

    public static final ModConfigSpec.BooleanValue SPAWNER_REQUIRE_POWER = BUILDER
            .comment("If true, the Reaper Spawner needs FE (from the Reaper Generator or any power mod) to run.")
            .define("spawner.requirePower", true);
    public static final ModConfigSpec.IntValue SPAWNER_ENERGY_CAPACITY = BUILDER
            .comment("FE the Reaper Spawner can store.")
            .defineInRange("spawner.energyCapacity", 100_000, 1000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SPAWNER_ENERGY_PER_SPAWN = BUILDER
            .comment("Base FE for each spawn. The spawner charges this up while its timer fills, then spawns.")
            .defineInRange("spawner.energyPerSpawn", 2000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SPAWNER_ENERGY_PER_MOB = BUILDER
            .comment("Extra FE added to each spawn's cost for every mob it spawns (Count cards raise this).")
            .defineInRange("spawner.energyPerMob", 400, 0, Integer.MAX_VALUE);

    // ---- Reaper Generator ----
    public static final ModConfigSpec.IntValue GENERATOR_CAPACITY = BUILDER
            .comment("FE the Reaper Generator can store.")
            .defineInRange("generator.capacity", 1_000_000, 1000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue GENERATOR_MAX_OUTPUT = BUILDER
            .comment("Most FE per tick the Reaper Generator pushes out of each energy side.")
            .defineInRange("generator.maxOutputPerSide", 10_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue GENERATOR_FUEL_MULTIPLIER = BUILDER
            .comment("Multiplies the FE per tick of every fuel. Fuel values themselves live in the data map",
                    "data/grimsoul/data_maps/item/generator_fuel.json (add modded mob drops with a datapack).")
            .defineInRange("generator.fuelMultiplier", 1.0, 0.0, 1000.0);

    // ---- Energy Cell ----
    public static final ModConfigSpec.IntValue CELL_CAPACITY = BUILDER
            .comment("FE one Reaper Energy Cell can store.")
            .defineInRange("energyCell.capacity", 10_000_000, 1000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue CELL_TRANSFER = BUILDER
            .comment("Most FE per tick an Energy Cell takes in, and pushes out of each output side.")
            .defineInRange("energyCell.maxTransfer", 50_000, 1, Integer.MAX_VALUE);

    // ---- Data Cores and Simulation Chamber ----
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> CORE_TIER_DATA = BUILDER
            .comment("Total data a Data Core needs to reach Whispering, Restless, Wraithbound and Soulforged.")
            .defineList("dataCore.dataPerTier", java.util.List.of(25, 150, 600, 2500), () -> 10, o -> o instanceof Integer i && i > 0);
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> CORE_TIER_CHANCE = BUILDER
            .comment("Virtualization success chance in percent for Hollow, Whispering, Restless, Wraithbound and Soulforged cores.")
            .defineList("dataCore.successChance", java.util.List.of(10, 40, 65, 85, 100), () -> 50, o -> o instanceof Integer i && i >= 0 && i <= 100);
    public static final ModConfigSpec.BooleanValue CORE_ALLOW_BOSSES = BUILDER
            .comment("If true, Data Cores can be bound to bosses (Wither, Ender Dragon, modded bosses in c:bosses).")
            .define("dataCore.allowBosses", false);
    public static final ModConfigSpec.IntValue SIM_CAPACITY = BUILDER
            .comment("FE the Simulation Chamber can store.")
            .defineInRange("simulation.energyCapacity", 1_000_000, 1000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue SIM_BASE_INTERVAL = BUILDER
            .comment("Ticks per simulation with no Speed cards.")
            .defineInRange("simulation.baseInterval", 200, 1, 72000);
    public static final ModConfigSpec.IntValue SIM_MIN_INTERVAL = BUILDER
            .comment("Ticks per simulation with a full stack of Speed cards.")
            .defineInRange("simulation.minInterval", 20, 1, 72000);
    public static final ModConfigSpec.IntValue SIM_BASE_COST = BUILDER
            .comment("FE per simulation for a mob with 20 health. Tougher mobs cost more (scaled by max health).")
            .defineInRange("simulation.baseCost", 16_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue SIM_MAX_EFFICIENCY = BUILDER
            .comment("How much of the power cost a full set of Efficiency cards removes (0.75 = 75% cheaper).")
            .defineInRange("simulation.maxEfficiency", 0.75, 0.0, 1.0);
    public static final ModConfigSpec.IntValue SIM_DATA_PER_RUN = BUILDER
            .comment("Data a core gains from each successful simulation (0 = cores only level from real kills).")
            .defineInRange("simulation.dataPerRun", 1, 0, 1000);

    public static final ModConfigSpec.BooleanValue GIVE_GUIDE_ON_JOIN = BUILDER
            .comment("If true, every player gets a Grim Soul Codex (the in-game guide book) the first time they join a world.")
            .define("guide.giveOnFirstJoin", true);

    public static final ModConfigSpec.DoubleValue APOTH_GEAR_CHANCE = BUILDER
            .comment("Apotheosis only: chance (0-1) that a hostile mob killed with a Player Kill card (Reaper Block or",
                    "Virtualization Chamber) also drops a random Apotheosis affix weapon, tool or armor piece.",
                    "Its rarity follows the nearest player's World Tier and luck. 0 turns this off.")
            .defineInRange("compat.apotheosisGearChance", 0.05, 0.0, 1.0);

    // ---- Soul Forge ----
    public static final ModConfigSpec.IntValue FORGE_ENERGY_CAPACITY = BUILDER
            .comment("FE the Soul Forge can store.")
            .defineInRange("soulForge.energyCapacity", 200_000, 1000, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue FORGE_EGG_TIME = BUILDER
            .comment("Ticks for a spawn egg craft (20 ticks = 1 second).")
            .defineInRange("soulForge.eggTime", 200, 1, 72000);
    public static final ModConfigSpec.IntValue FORGE_EGG_ENERGY = BUILDER
            .comment("Total FE for a spawn egg craft, spread evenly over its time.")
            .defineInRange("soulForge.eggEnergy", 60_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue FORGE_BOSS_TIME = BUILDER
            .comment("Ticks for a Boss Core craft.")
            .defineInRange("soulForge.bossTime", 1200, 1, 72000);
    public static final ModConfigSpec.IntValue FORGE_BOSS_ENERGY = BUILDER
            .comment("Total FE for a Boss Core craft.")
            .defineInRange("soulForge.bossEnergy", 300_000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue FORGE_SOULBOUND_TIME = BUILDER
            .comment("Ticks for a soulbound item craft.")
            .defineInRange("soulForge.soulboundTime", 100, 1, 72000);
    public static final ModConfigSpec.IntValue FORGE_SOULBOUND_ENERGY = BUILDER
            .comment("Total FE for a soulbound item craft.")
            .defineInRange("soulForge.soulboundEnergy", 20_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue FORGE_MAX_INPUT = BUILDER
            .comment("Most FE per tick the Soul Forge accepts from cables and generators.")
            .defineInRange("soulForge.maxInputPerTick", 10_000, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue FORGE_EGG_ENABLED = BUILDER
            .comment("If false, the Soul Forge won't make spawn eggs from Soulforged cores.")
            .define("soulForge.eggEnabled", true);
    public static final ModConfigSpec.BooleanValue FORGE_BOSS_ENABLED = BUILDER
            .comment("If false, the Soul Forge won't make Boss Core cards.")
            .define("soulForge.bossEnabled", true);
    public static final ModConfigSpec.BooleanValue FORGE_SOULBOUND_ENABLED = BUILDER
            .comment("If false, the Soul Forge won't make soulbound items.")
            .define("soulForge.soulboundEnabled", true);

    // ---- Per-block tuning: every mod block gets its own hardness and blast resistance ----
    /** Every block this mod registers, by its registry path. */
    public static final java.util.List<String> BLOCK_NAMES = java.util.List.of(
            "reaper_block", "mob_fan", "xp_tank", "essence_spawner", "reaper_generator", "simulation_chamber",
            "output_crate", "energy_cell", "soul_forge", "soul_forge_casing");
    public static final java.util.Map<String, ModConfigSpec.DoubleValue> BLOCK_HARDNESS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, ModConfigSpec.DoubleValue> BLOCK_BLAST_RESISTANCE = new java.util.LinkedHashMap<>();
    static {
        BUILDER.push("blocks");
        for (String name : BLOCK_NAMES) {
            BUILDER.push(name);
            BLOCK_HARDNESS.put(name, BUILDER
                    .comment("How long this block takes to break. Vanilla stone is 1.5, iron block 5.0, obsidian 50.")
                    .defineInRange("hardness", 3.5, 0.0, 1000.0));
            BLOCK_BLAST_RESISTANCE.put(name, BUILDER
                    .comment("How well this block survives explosions. Vanilla stone is 6.0, obsidian 1200.")
                    .defineInRange("blastResistance", 6.0, 0.0, 10000.0));
            BUILDER.pop();
        }
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
