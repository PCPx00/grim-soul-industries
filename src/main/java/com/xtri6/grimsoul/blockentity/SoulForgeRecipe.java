package com.xtri6.grimsoul.blockentity;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.SettingsCardItem;
import com.xtri6.grimsoul.item.UpgradeCardItem;
import com.xtri6.grimsoul.item.UpgradeType;

import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * The three things the Soul Forge makes. Each takes one Soul Steel plus the item in the work slot.
 *
 * @param time        ticks the forge works on it
 * @param totalEnergy FE the whole craft costs, drawn evenly over its time
 */
public record SoulForgeRecipe(Kind kind, int time, int totalEnergy) {
    public enum Kind { SPAWN_EGG, BOSS_CORE, SOULBOUND }

    /** Spawn egg of a Soulforged core's mob (mobs that have an egg). */
    public static final SoulForgeRecipe EGG = new SoulForgeRecipe(Kind.SPAWN_EGG, 200, 60_000);
    /** Boss Core card from a Soulforged boss core. */
    public static final SoulForgeRecipe BOSS = new SoulForgeRecipe(Kind.BOSS_CORE, 1200, 300_000);
    /** Soulbound copy of a single item. */
    public static final SoulForgeRecipe SOULBOUND = new SoulForgeRecipe(Kind.SOULBOUND, 100, 20_000);

    public int perTick() {
        return Math.max(1, totalEnergy / time);
    }

    /** The recipe for this work item, or null when the forge can't make anything from it. */
    @Nullable
    public static SoulForgeRecipe find(ItemStack work) {
        if (work.isEmpty()) {
            return null;
        }
        if (work.getItem() instanceof DataCoreItem) {
            if (DataCoreItem.getTier(work) < DataCoreItem.MAX_TIER) {
                return null;
            }
            EntityType<?> mob = DataCoreItem.getMob(work).orElse(null);
            if (mob == null) {
                return null;
            }
            if (DataCoreItem.isBoss(mob)) {
                return BOSS;
            }
            return SpawnEggItem.byId(mob) == null ? null : EGG;
        }
        boolean cannotBind = work.getCount() != 1 || work.getItem() instanceof UpgradeCardItem
                || work.getItem() instanceof SettingsCardItem || work.has(Registration.SOULBOUND.get());
        return cannotBind ? null : SOULBOUND;
    }

    /** What this craft produces from the work item. Only call it when {@link #find} returned this recipe. */
    public ItemStack result(ItemStack work) {
        return switch (kind) {
            case SPAWN_EGG -> new ItemStack(SpawnEggItem.byId(DataCoreItem.getMob(work).orElseThrow()));
            case BOSS_CORE -> new ItemStack(Registration.CARDS.get(UpgradeType.BOSS_CORE).get());
            case SOULBOUND -> {
                ItemStack copy = work.copyWithCount(1);
                copy.set(Registration.SOULBOUND.get(), Unit.INSTANCE);
                yield copy;
            }
        };
    }
}
