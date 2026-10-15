package com.xtri6.grimsoul.compat.emi;

import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.generator.GeneratorFuel;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * EMI support. Loaded only when EMI is installed. Shows the same Reaper Generator fuel list as the
 * JEI page: which items it burns, how much power each makes and for how long.
 */
@EmiEntrypoint
public class GrimSoulEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory FUEL = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID, "generator_fuel"),
            EmiStack.of(Registration.REAPER_GENERATOR.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(FUEL);
        registry.addWorkstation(FUEL, EmiStack.of(Registration.REAPER_GENERATOR.get()));
        // Fuel values come from datapack data, which only exists once the game has loaded it.
        registry.addDeferredRecipes(consumer -> {
            for (var holder : BuiltInRegistries.ITEM.holders().toList()) {
                GeneratorFuel fuel = holder.getData(GeneratorFuel.DATA_MAP);
                if (fuel != null) {
                    Item item = holder.value();
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GrimSoul.MODID,
                            "generator_fuel/" + BuiltInRegistries.ITEM.getKey(item).getNamespace() + "/"
                                    + BuiltInRegistries.ITEM.getKey(item).getPath());
                    consumer.accept(new FuelEmiRecipe(id, item, fuel));
                }
            }
        });
    }
}
