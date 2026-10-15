package com.xtri6.grimsoul.compat.jei;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SoulForgeRecipe;
import com.xtri6.grimsoul.client.GuiStyle;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Soul Forge page in JEI: the work item, Soul Steel, the result, and what the craft costs. */
public class SoulForgeCategory implements IRecipeCategory<SoulForgeRecipe.Example> {
    private final IDrawable background;
    private final IDrawable icon;

    public SoulForgeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 40);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Registration.SOUL_FORGE_ITEM.get()));
    }

    @Override
    public RecipeType<SoulForgeRecipe.Example> getRecipeType() {
        return GrimSoulJeiPlugin.FORGE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.grimsoul.soul_forge");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SoulForgeRecipe.Example example, IFocusGroup focuses) {
        builder.addInputSlot(4, 4).setStandardSlotBackground().addItemStack(example.input());
        builder.addInputSlot(4, 22).setStandardSlotBackground().addItemStack(new ItemStack(Registration.SOUL_STEEL.get()));
        builder.addOutputSlot(120, 12).setStandardSlotBackground().addItemStack(example.output());
    }

    @Override
    public void draw(SoulForgeRecipe.Example example, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        var recipe = example.recipe();
        if (recipe.kind() == SoulForgeRecipe.Kind.SOULBOUND) {
            graphics.drawString(font, Component.translatable("jei.grimsoul.forge_any").withStyle(ChatFormatting.GOLD),
                    26, 4, 0xFFFFFF, false);
        }
        graphics.drawString(font, Component.translatable("jei.grimsoul.forge_time",
                String.format("%.1f", recipe.time() / 20.0), GuiStyle.compact(recipe.totalEnergy())).withStyle(ChatFormatting.GRAY),
                26, 22, 0xFFFFFF, false);
    }
}
