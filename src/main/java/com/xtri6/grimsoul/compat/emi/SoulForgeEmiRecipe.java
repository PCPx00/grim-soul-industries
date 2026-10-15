package com.xtri6.grimsoul.compat.emi;

import java.util.List;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SoulForgeRecipe;
import com.xtri6.grimsoul.client.GuiStyle;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** One Soul Forge craft in EMI: work item, Soul Steel, the result, and what the craft costs. */
public record SoulForgeEmiRecipe(ResourceLocation id, SoulForgeRecipe.Example example) implements EmiRecipe {
    @Override
    public EmiRecipeCategory getCategory() {
        return GrimSoulEmiPlugin.FORGE;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(EmiStack.of(example.input()), EmiStack.of(new ItemStack(Registration.SOUL_STEEL.get())));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(EmiStack.of(example.output()));
    }

    @Override
    public int getDisplayWidth() {
        return 160;
    }

    @Override
    public int getDisplayHeight() {
        return 40;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        SoulForgeRecipe recipe = example.recipe();
        widgets.addSlot(EmiStack.of(example.input()), 4, 4);
        widgets.addSlot(EmiStack.of(new ItemStack(Registration.SOUL_STEEL.get())), 4, 22);
        widgets.addFillingArrow(50, 12, recipe.time());
        widgets.addSlot(EmiStack.of(example.output()), 120, 12).recipeContext(this);
        if (recipe.kind() == SoulForgeRecipe.Kind.SOULBOUND) {
            widgets.addText(Component.translatable("jei.grimsoul.forge_any").withStyle(ChatFormatting.GOLD), 26, 4, 0xFFFFFF, false);
        }
        widgets.addText(Component.translatable("jei.grimsoul.forge_time",
                String.format("%.1f", recipe.time() / 20.0), GuiStyle.compact(recipe.totalEnergy()))
                .withStyle(ChatFormatting.GRAY), 26, 30, 0xFFFFFF, false);
    }
}
