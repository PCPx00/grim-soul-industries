package com.xtri6.grimsoul.client;

import com.xtri6.grimsoul.blockentity.CardMachineBlockEntity;
import com.xtri6.grimsoul.blockentity.MobFanBlockEntity;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.CardMachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Mob Fan GUI: Range card slot, push-area length / width / height, and power / redstone / area switches. */
public class MobFanScreen extends AbstractContainerScreen<CardMachineMenu> {
    private static final int[] ROW_Y = {48, 66, 84};
    private static final String[] ROW_KEYS = {"length", "width", "height"};

    private Button areaButton;
    private Button powerButton;
    private Button redstoneButton;

    public MobFanScreen(CardMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = CardMachineMenu.WIDTH;
        this.imageHeight = 216;
        this.inventoryLabelY = CardMachineMenu.FAN_INV_Y - 11;
        this.inventoryLabelX = GuiStyle.invX(CardMachineMenu.WIDTH);
        this.titleLabelX = 12;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 26, topPos + 5));
        int x = leftPos;
        int y = topPos;
        for (int row = 0; row < 3; row++) {
            Tooltip tip = Tooltip.create(Component.translatable("gui.grimsoul.fan_" + ROW_KEYS[row] + ".tooltip"));
            int minusId = MobFanBlockEntity.BTN_LENGTH + row * 2;
            small(x + 76, y + ROW_Y[row], "-", minusId, tip);
            small(x + 120, y + ROW_Y[row], "+", minusId + 1, tip);
        }
        Button max = button(x + 12, y + 104, 120, MobFanBlockEntity.BTN_MAX, "fan_max");
        max.setMessage(Component.translatable("gui.grimsoul.fan_max"));
        areaButton = button(x + 158, y + 46, 80, MobFanBlockEntity.BTN_SHOW_AREA, "fan_area");
        powerButton = button(x + 158, y + 66, 80, MobFanBlockEntity.BTN_POWER, "power");
        redstoneButton = button(x + 158, y + 86, 80, MobFanBlockEntity.BTN_REDSTONE, "redstone");
        updateLabels();
    }

    private Button button(int x, int y, int width, int id, String key) {
        return addRenderableWidget(Button.builder(Component.empty(), b -> press(id))
                .bounds(x, y, width, 14)
                .tooltip(Tooltip.create(Component.translatable("gui.grimsoul." + key + ".tooltip")))
                .build());
    }

    private void small(int x, int y, String label, int id, Tooltip tip) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> press(id)).bounds(x, y, 12, 12).tooltip(tip).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLabels();
    }

    private void updateLabels() {
        if (areaButton == null) {
            return;
        }
        areaButton.setMessage(ReaperScreen.onOff("show_area", menu.getData(MobFanBlockEntity.DATA_SHOW_AREA) != 0));
        powerButton.setMessage(ReaperScreen.onOff("power", menu.getData(MobFanBlockEntity.DATA_ENABLED) != 0));
        RedstoneMode mode = RedstoneMode.byIndex(menu.getData(MobFanBlockEntity.DATA_REDSTONE));
        redstoneButton.setMessage(Component.translatable("gui.grimsoul.redstone." + mode.id()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        GuiStyle.panel(graphics, x, y, x + imageWidth, y + imageHeight);
        for (Slot slot : menu.slots) {
            GuiStyle.slot(graphics, x + slot.x, y + slot.y, GuiStyle.SLOT_BG);
        }
        Slot card = menu.slots.get(0);
        if (!card.hasItem()) {
            GuiStyle.ghost(graphics, new net.minecraft.world.item.ItemStack(com.xtri6.grimsoul.Registration.CARDS
                    .get(com.xtri6.grimsoul.item.UpgradeType.RANGE).get()), x + card.x, y + card.y);
        }
        graphics.fill(x + 8, y + 42, x + imageWidth - 8, y + 43, GuiStyle.PANEL_METAL);
        int light = menu.getData(MobFanBlockEntity.DATA_ENABLED) != 0 ? GuiStyle.ACCENT_GLOW : GuiStyle.PANEL_METAL;
        graphics.fill(x + imageWidth - 12, y + 7, x + imageWidth - 6, y + 13, light);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.range_cards",
                menu.getData(MobFanBlockEntity.DATA_CARDS), CardMachineBlockEntity.MAX_RANGE_CARDS), 34, CardMachineMenu.FAN_CARD_Y, GuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.fan_limits",
                menu.getData(MobFanBlockEntity.DATA_MAX_LENGTH), menu.getData(MobFanBlockEntity.DATA_MAX_WIDTH),
                menu.getData(MobFanBlockEntity.DATA_MAX_HEIGHT)), 34, CardMachineMenu.FAN_CARD_Y + 10, GuiStyle.TEXT_DIM, false);
        int[] values = {
                menu.getData(MobFanBlockEntity.DATA_LENGTH),
                menu.getData(MobFanBlockEntity.DATA_WIDTH),
                menu.getData(MobFanBlockEntity.DATA_HEIGHT)};
        for (int row = 0; row < 3; row++) {
            graphics.drawString(font, Component.translatable("gui.grimsoul." + ROW_KEYS[row]), 12, ROW_Y[row] + 2, GuiStyle.TEXT_DIM, false);
            GuiStyle.centered(graphics, font, String.valueOf(values[row]), 104, ROW_Y[row] + 2, GuiStyle.TEXT);
        }
    }
}
