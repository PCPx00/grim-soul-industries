package com.xtri6.grimsoul.compat.jei;

import com.xtri6.grimsoul.GrimSoul;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.client.GuiStyle;
import com.xtri6.grimsoul.compat.jei.GrimSoulJeiPlugin.FuelEntry;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One row per burnable item: the item on the left, its power and burn time on the right. */
public class FuelCategory implements IRecipeCategory<FuelEntry> {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 22;
    private final IDrawable background;
    private final IDrawable icon;

    public FuelCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(Registration.REAPER_GENERATOR.get());
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<FuelEntry> getRecipeType() {
        return GrimSoulJeiPlugin.FUEL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.grimsoul.generator_fuel");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FuelEntry entry, IFocusGroup focuses) {
        builder.addInputSlot(2, 3).setStandardSlotBackground().addItemStack(entry.stack());
    }

    @Override
    public void draw(FuelEntry entry, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        var fuel = entry.fuel();
        graphics.drawString(font, Component.translatable("jei.grimsoul.fuel_rate", fuel.fePerTick())
                .withStyle(ChatFormatting.GOLD), 24, 4, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("jei.grimsoul.fuel_total",
                String.format("%.1f", fuel.burnTime() / 20.0), GuiStyle.compact((int) Math.min(Integer.MAX_VALUE, fuel.totalFe()))).withStyle(ChatFormatting.GRAY),
                24, 13, 0xFFFFFF, false);
    }
}
