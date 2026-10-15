package com.xtri6.grimsoul.compat.emi;

import java.util.List;

import com.xtri6.grimsoul.client.GuiStyle;
import com.xtri6.grimsoul.generator.GeneratorFuel;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** One burnable item in the Reaper Generator fuel list. */
public record FuelEmiRecipe(ResourceLocation id, Item item, GeneratorFuel fuel) implements EmiRecipe {
    @Override
    public EmiRecipeCategory getCategory() {
        return GrimSoulEmiPlugin.FUEL;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(EmiStack.of(item));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 22;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(EmiStack.of(item), 2, 3);
        widgets.addText(Component.translatable("jei.grimsoul.fuel_rate", fuel.fePerTick())
                .withStyle(ChatFormatting.GOLD), 24, 4, 0xFFFFFF, false);
        widgets.addText(Component.translatable("jei.grimsoul.fuel_total",
                String.format("%.1f", fuel.burnTime() / 20.0),
                GuiStyle.compact((int) Math.min(Integer.MAX_VALUE, fuel.totalFe())))
                .withStyle(ChatFormatting.GRAY), 24, 13, 0xFFFFFF, false);
    }
}
