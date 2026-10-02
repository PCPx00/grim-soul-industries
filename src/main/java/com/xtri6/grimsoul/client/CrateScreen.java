package com.xtri6.grimsoul.client;

import com.xtri6.grimsoul.blockentity.OutputCrateBlockEntity;
import com.xtri6.grimsoul.menu.CrateMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Output Crate GUI: a 54-slot chest with a Void Overflow switch. */
public class CrateScreen extends AbstractContainerScreen<CrateMenu> {
    private Button voidButton;

    public CrateScreen(CrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = CrateMenu.WIDTH;
        this.inventoryLabelX = GuiStyle.invX(CrateMenu.WIDTH);
        this.titleLabelX = GuiStyle.invX(CrateMenu.WIDTH);
        this.imageHeight = 242;
        this.inventoryLabelY = CrateMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 16, topPos + 5));
        voidButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, OutputCrateBlockEntity.BTN_VOID);
                    }
                })
                .bounds(leftPos + CrateMenu.WIDTH - 19 - 76, topPos + 128, 76, 14)
                .tooltip(Tooltip.create(Component.translatable("gui.grimsoul.crate_void.tooltip")))
                .build());
        updateLabels();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    private void updateLabels() {
        if (voidButton != null) {
            voidButton.setMessage(ReaperScreen.onOff("crate_void", menu.getData(OutputCrateBlockEntity.DATA_VOID) != 0));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyle.panel(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        for (Slot slot : menu.slots) {
            GuiStyle.slot(graphics, leftPos + slot.x, topPos + slot.y, GuiStyle.SLOT_BG);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.slots",
                menu.getData(OutputCrateBlockEntity.DATA_USED), OutputCrateBlockEntity.SLOTS), GuiStyle.invX(CrateMenu.WIDTH), 131, GuiStyle.TEXT_DIM, false);
    }
}
