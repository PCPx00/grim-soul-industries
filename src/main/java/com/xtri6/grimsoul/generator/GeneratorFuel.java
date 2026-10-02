package com.xtri6.grimsoul.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xtri6.grimsoul.GrimSoul;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * How much power one mob drop makes in the Reaper Generator: fePerTick for burnTime ticks.
 * Set per item (or item tag) in data/grimsoul/data_maps/item/generator_fuel.json, so pack devs
 * can change values or add modded drops with a datapack.
 */
public record GeneratorFuel(int fePerTick, int burnTime) {
    public static final Codec<GeneratorFuel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("fe_per_tick").forGetter(GeneratorFuel::fePerTick),
            ExtraCodecs.POSITIVE_INT.fieldOf("burn_time").forGetter(GeneratorFuel::burnTime)
    ).apply(instance, GeneratorFuel::new));

    public static final DataMapType<Item, GeneratorFuel> DATA_MAP = DataMapType
            .builder(ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "generator_fuel"), Registries.ITEM, CODEC)
            .synced(CODEC, false)
            .build();

    /** The fuel value of this stack, or null if the generator can't burn it. */
    public static GeneratorFuel of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItemHolder().getData(DATA_MAP);
    }

    public long totalFe() {
        return (long) fePerTick * burnTime;
    }
}
