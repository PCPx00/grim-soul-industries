package com.xtri6.grimsoul.blockentity;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.xtri6.grimsoul.Config;
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
    public static SoulForgeRecipe egg() {
        return new SoulForgeRecipe(Kind.SPAWN_EGG, Config.FORGE_EGG_TIME.get(), Config.FORGE_EGG_ENERGY.get());
    }

    /** Boss Core card from a Soulforged boss core. */
    public static SoulForgeRecipe boss() {
        return new SoulForgeRecipe(Kind.BOSS_CORE, Config.FORGE_BOSS_TIME.get(), Config.FORGE_BOSS_ENERGY.get());
    }

    /** Soulbound copy of a single item. */
    public static SoulForgeRecipe soulbound() {
        return new SoulForgeRecipe(Kind.SOULBOUND, Config.FORGE_SOULBOUND_TIME.get(), Config.FORGE_SOULBOUND_ENERGY.get());
    }

    /** One example input and what it makes, for the JEI and EMI pages. Input is the work-slot item. */
    public record Example(ItemStack input, ItemStack output, SoulForgeRecipe recipe) {
    }

    /** Example crafts for the recipe viewers: an egg for every spawn egg, the boss cards and one soulbound item. */
    public static java.util.List<Example> examples() {
        java.util.List<Example> list = new java.util.ArrayList<>();
        for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.holders().toList()) {
            if (holder.value() instanceof SpawnEggItem egg && Config.FORGE_EGG_ENABLED.get()) {
                EntityType<?> mob = egg.getType(new ItemStack(egg));
                ItemStack core = new ItemStack(Registration.DATA_CORE.get());
                core.set(Registration.DATA_CORE_MOB.get(), net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob));
                core.set(Registration.DATA_CORE_DATA.get(), DataCoreItem.threshold(DataCoreItem.MAX_TIER));
                list.add(new Example(core, new ItemStack(egg), egg()));
            }
        }
        for (EntityType<?> boss : Config.FORGE_BOSS_ENABLED.get()
                ? java.util.List.of(EntityType.WITHER, EntityType.ENDER_DRAGON) : java.util.List.<EntityType<?>>of()) {
            ItemStack core = new ItemStack(Registration.DATA_CORE.get());
            core.set(Registration.DATA_CORE_MOB.get(), net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss));
            core.set(Registration.DATA_CORE_DATA.get(), DataCoreItem.threshold(DataCoreItem.MAX_TIER));
            list.add(new Example(core, new ItemStack(Registration.CARDS.get(UpgradeType.BOSS_CORE).get()), boss()));
        }
        if (Config.FORGE_SOULBOUND_ENABLED.get()) {
            list.add(new Example(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD),
                    soulboundCopy(new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)), soulbound()));
        }
        return list;
    }

    private static ItemStack soulboundCopy(ItemStack work) {
        ItemStack copy = work.copyWithCount(1);
        copy.set(Registration.SOULBOUND.get(), Unit.INSTANCE);
        return copy;
    }

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
                return Config.FORGE_BOSS_ENABLED.get() ? boss() : null;
            }
            if (SpawnEggItem.byId(mob) == null || !Config.FORGE_EGG_ENABLED.get()) {
                return null;
            }
            return egg();
        }
        boolean cannotBind = work.getCount() != 1 || work.getItem() instanceof UpgradeCardItem
                || work.getItem() instanceof SettingsCardItem || work.has(Registration.SOULBOUND.get());
        return cannotBind || !Config.FORGE_SOULBOUND_ENABLED.get() ? null : soulbound();
    }

    /** What this craft produces from the work item. Only call it when {@link #find} returned this recipe. */
    public ItemStack result(ItemStack work) {
        return switch (kind) {
            case SPAWN_EGG -> new ItemStack(SpawnEggItem.byId(DataCoreItem.getMob(work).orElseThrow()));
            case BOSS_CORE -> new ItemStack(Registration.CARDS.get(UpgradeType.BOSS_CORE).get());
            case SOULBOUND -> soulboundCopy(work);
        };
    }
}
