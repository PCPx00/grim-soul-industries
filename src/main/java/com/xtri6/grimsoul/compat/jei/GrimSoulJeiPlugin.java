package com.xtri6.grimsoul.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.generator.GeneratorFuel;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI support. Loaded only when JEI is installed. Shows which items the Reaper Generator burns,
 * how much power each makes and for how long, and marks the generator as the machine for that page.
 * Recipes for the machines themselves already show up in JEI from their normal crafting recipes.
 */
@JeiPlugin
public class GrimSoulJeiPlugin implements IModPlugin {
    public static final RecipeType<FuelEntry> FUEL = RecipeType.create(GrimSoul.MODID, "generator_fuel", FuelEntry.class);
    public static final RecipeType<com.xtri6.grimsoul.blockentity.SoulForgeRecipe.Example> FORGE =
            RecipeType.create(GrimSoul.MODID, "soul_forge", com.xtri6.grimsoul.blockentity.SoulForgeRecipe.Example.class);

    /** One burnable item and its fuel value. */
    public record FuelEntry(ItemStack stack, GeneratorFuel fuel) {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FuelCategory(registration.getJeiHelpers().getGuiHelper()),
                new SoulForgeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<FuelEntry> entries = new ArrayList<>();
        BuiltInRegistries.ITEM.holders().forEach(holder -> {
            GeneratorFuel fuel = holder.getData(GeneratorFuel.DATA_MAP);
            if (fuel != null) {
                entries.add(new FuelEntry(new ItemStack(holder.value()), fuel));
            }
        });
        entries.sort((a, b) -> Long.compare(b.fuel().totalFe(), a.fuel().totalFe()));
        registration.addRecipes(FUEL, entries);
        registration.addRecipes(FORGE, com.xtri6.grimsoul.blockentity.SoulForgeRecipe.examples());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Registration.REAPER_GENERATOR.get(), FUEL);
        registration.addRecipeCatalyst(Registration.SOUL_FORGE.get(), FORGE);
    }

}
