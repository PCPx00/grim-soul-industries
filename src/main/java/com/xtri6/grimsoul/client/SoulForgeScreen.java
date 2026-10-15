package com.xtri6.grimsoul.client;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.blockentity.SoulForgeBlockEntity;
import com.xtri6.grimsoul.menu.SoulForgeMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Soul Forge GUI: the work and steel slots, the result, a progress arrow, power, and the structure check. */
public class SoulForgeScreen extends AbstractContainerScreen<SoulForgeMenu> {
    private static final int ARROW_X1 = 66, ARROW_X2 = 108, ARROW_Y = 50;
    private static final int POWER_X1 = 152, POWER_X2 = 160, POWER_Y1 = 18, POWER_Y2 = 78;

    public SoulForgeScreen(SoulForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SoulForgeMenu.WIDTH;
        this.imageHeight = SoulForgeMenu.HEIGHT;
        this.inventoryLabelX = SoulForgeMenu.INV_X;
        this.inventoryLabelY = SoulForgeMenu.INV_Y - 11;
        this.titleLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 26, topPos + 5));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiStyle.panel(g, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        GuiStyle.slot(g, leftPos + SoulForgeMenu.WORK_X, topPos + SoulForgeMenu.WORK_Y, GuiStyle.SLOT_BG);
        GuiStyle.slot(g, leftPos + SoulForgeMenu.STEEL_X, topPos + SoulForgeMenu.STEEL_Y, GuiStyle.SLOT_BG);
        GuiStyle.slot(g, leftPos + SoulForgeMenu.OUTPUT_X, topPos + SoulForgeMenu.OUTPUT_Y, GuiStyle.SLOT_BG);

        int max = menu.getData(SoulForgeBlockEntity.DATA_MAX);
        int progress = menu.getData(SoulForgeBlockEntity.DATA_PROGRESS);
        float done = max > 0 ? progress / (float) max : 0.0F;
        GuiStyle.bar(g, leftPos + ARROW_X1, topPos + ARROW_Y, leftPos + ARROW_X2, topPos + ARROW_Y + 6, done);

        // Power: a tall bar that fills from the bottom.
        int x1 = leftPos + POWER_X1, x2 = leftPos + POWER_X2;
        int y1 = topPos + POWER_Y1, y2 = topPos + POWER_Y2;
        g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, GuiStyle.a(GuiStyle.SLOT_DARK_EDGE));
        g.fill(x1, y1, x2, y2, GuiStyle.a(GuiStyle.BAR_BG));
        int filled = y2 - Math.round((y2 - y1) * powerFraction());
        g.fill(x1, filled, x2, y2, GuiStyle.a(GuiStyle.BAR_FILL));
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int missing = menu.getData(SoulForgeBlockEntity.DATA_MISSING);
        Component structure = menu.getData(SoulForgeBlockEntity.DATA_FORMED) == 1
                ? Component.translatable("gui.grimsoul.soul_forge.structure_formed")
                : Component.translatable("gui.grimsoul.soul_forge.structure_missing", missing);
        g.drawString(font, structure, 8, 20, menu.getData(SoulForgeBlockEntity.DATA_FORMED) == 1
                ? GuiStyle.TEXT_OK : GuiStyle.TEXT_WARN, false);
        g.drawString(font, Component.translatable(statusKey(menu.getData(SoulForgeBlockEntity.DATA_STATUS))),
                8, 30, GuiStyle.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x1 = leftPos + POWER_X1, x2 = leftPos + POWER_X2;
        int y1 = topPos + POWER_Y1, y2 = topPos + POWER_Y2;
        if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
            g.renderTooltip(font, Component.translatable("gui.grimsoul.soul_forge.energy",
                    GuiStyle.compact(energy()), GuiStyle.compact(Config.FORGE_ENERGY_CAPACITY.get())), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    private int energy() {
        return menu.getSplit(SoulForgeBlockEntity.DATA_ENERGY_LO);
    }

    private float powerFraction() {
        return Math.min(1.0F, energy() / (float) Config.FORGE_ENERGY_CAPACITY.get());
    }

    private static String statusKey(int status) {
        return switch (status) {
            case SoulForgeBlockEntity.STATUS_CRAFTING -> "gui.grimsoul.soul_forge.status.crafting";
            case SoulForgeBlockEntity.STATUS_INCOMPLETE -> "gui.grimsoul.soul_forge.status.incomplete";
            case SoulForgeBlockEntity.STATUS_NO_WORK -> "gui.grimsoul.soul_forge.status.no_work";
            case SoulForgeBlockEntity.STATUS_NO_STEEL -> "gui.grimsoul.soul_forge.status.no_steel";
            case SoulForgeBlockEntity.STATUS_NO_POWER -> "gui.grimsoul.soul_forge.status.no_power";
            case SoulForgeBlockEntity.STATUS_OUTPUT_FULL -> "gui.grimsoul.soul_forge.status.output_full";
            case SoulForgeBlockEntity.STATUS_NO_RECIPE -> "gui.grimsoul.soul_forge.status.no_recipe";
            default -> "gui.grimsoul.soul_forge.status.idle";
        };
    }
}
