package com.xtri6.grimsoul.compat;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import com.xtri6.grimsoul.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Optional Apotheosis support, done through reflection so Grim Soul doesn't need Apotheosis to run.
 *
 * Apotheosis only hands out its affix gear for kills by a real player, so mobs killed by a machine
 * never drop any. With a Player Kill card, hostile mobs killed by the Reaper Block or simulated in the
 * Virtualization Chamber get a chance to drop a random affix weapon, tool or armor piece instead,
 * rolled the same way Apotheosis rolls its own loot (rarity from the nearest player's World Tier and luck).
 */
public final class ApothCompat {
    private static final double PLAYER_SEARCH_RANGE = 128.0;

    private static boolean checked;
    @Nullable
    private static Method forPlayerAtPos;
    @Nullable
    private static Method createRandomLootItem;

    private ApothCompat() {
    }

    private static boolean ready() {
        if (!checked) {
            checked = true;
            if (ModList.get().isLoaded("apotheosis")) {
                try {
                    Class<?> genContext = Class.forName("dev.shadowsoffire.apotheosis.tiers.GenContext");
                    Class<?> rarity = Class.forName("dev.shadowsoffire.apotheosis.loot.LootRarity");
                    Class<?> controller = Class.forName("dev.shadowsoffire.apotheosis.loot.LootController");
                    forPlayerAtPos = genContext.getMethod("forPlayerAtPos", RandomSource.class, Player.class, BlockPos.class);
                    createRandomLootItem = controller.getMethod("createRandomLootItem", genContext, rarity);
                    LogUtils.getLogger().info("Grim Soul Industries: Apotheosis found, Player Kill cards can drop affix gear.");
                } catch (ReflectiveOperationException | LinkageError e) {
                    LogUtils.getLogger().warn("Grim Soul Industries: couldn't hook into Apotheosis, affix gear drops are off.", e);
                    forPlayerAtPos = null;
                    createRandomLootItem = null;
                }
            }
        }
        return createRandomLootItem != null;
    }

    /**
     * Rolls the affix gear drop for one Player Kill card kill. Returns an empty stack when Apotheosis
     * isn't installed, the mob isn't hostile, or the roll fails.
     *
     * @param killer the machine's fake player, used for the roll when no real player is nearby
     */
    public static ItemStack rollPlayerKillGear(ServerLevel level, LivingEntity victim, BlockPos machine, Player killer) {
        double chance = Config.APOTH_GEAR_CHANCE.get();
        if (chance <= 0 || !(victim instanceof Enemy) || !ready()) {
            return ItemStack.EMPTY;
        }
        RandomSource rand = level.random;
        if (rand.nextDouble() >= chance) {
            return ItemStack.EMPTY;
        }
        // Use the nearest real player, so the gear matches the World Tier they're playing on.
        Player player = level.getNearestPlayer(machine.getX() + 0.5, machine.getY() + 0.5, machine.getZ() + 0.5,
                PLAYER_SEARCH_RANGE, p -> !p.isSpectator());
        if (player == null) {
            player = killer;
        }
        try {
            Object ctx = forPlayerAtPos.invoke(null, rand, player, machine);
            Object stack = createRandomLootItem.invoke(null, ctx, null);
            return stack instanceof ItemStack item ? item : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("Grim Soul Industries: failed to roll Apotheosis gear, turning the drop off.", e);
            createRandomLootItem = null;
            return ItemStack.EMPTY;
        }
    }
}
