package com.xtri6.grimsoul.client;

import com.xtri6.grimsoul.blockentity.CardMachineBlockEntity;
import com.xtri6.grimsoul.blockentity.XpTankBlockEntity;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.CardMachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** XP Tank GUI: XP bar, Range card slot, magnet / power / redstone switches, and buttons to take XP. */
public class XpTankScreen extends AbstractContainerScreen<CardMachineMenu> {
    private static final int[] AREA_ROW_Y = {116, 132, 148};
    private static final String[] AXES = {"X", "Y", "Z"};

    private Button magnetButton;
    private Button areaButton;
    private Button powerButton;
    private Button redstoneButton;

    public XpTankScreen(CardMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = CardMachineMenu.WIDTH;
        this.imageHeight = 260;
        this.inventoryLabelY = CardMachineMenu.TANK_INV_Y - 11;
        this.inventoryLabelX = GuiStyle.invX(CardMachineMenu.WIDTH);
        this.titleLabelX = 12;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 26, topPos + 5));
        int x = leftPos;
        int y = topPos;
        magnetButton = button(x + 12, y + 64, 72, XpTankBlockEntity.BTN_MAGNET, "magnet");
        powerButton = button(x + 89, y + 64, 72, XpTankBlockEntity.BTN_POWER, "power");
        redstoneButton = button(x + 166, y + 64, 72, XpTankBlockEntity.BTN_REDSTONE, "redstone");
        Button takeAll = button(x + 12, y + 84, 111, XpTankBlockEntity.BTN_TAKE_ALL, "take_all");
        takeAll.setMessage(Component.translatable("gui.grimsoul.take_all"));
        Button takeLevel = button(x + 127, y + 84, 111, XpTankBlockEntity.BTN_TAKE_LEVEL, "take_level");
        takeLevel.setMessage(Component.translatable("gui.grimsoul.take_level"));
        for (int axis = 0; axis < 3; axis++) {
            Tooltip tip = Tooltip.create(Component.translatable("gui.grimsoul.tank_area.tooltip", AXES[axis]));
            int minusId = XpTankBlockEntity.BTN_AREA_BASE + axis * 2;
            small(x + 76, y + AREA_ROW_Y[axis], "-", minusId, tip);
            small(x + 120, y + AREA_ROW_Y[axis], "+", minusId + 1, tip);
        }
        areaButton = button(x + 158, y + 115, 80, XpTankBlockEntity.BTN_SHOW_AREA, "tank_show_area");
        Button max = button(x + 158, y + 133, 80, XpTankBlockEntity.BTN_AREA_MAX, "tank_area_max");
        max.setMessage(Component.translatable("gui.grimsoul.area_max"));
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
        if (magnetButton == null) {
            return;
        }
        magnetButton.setMessage(ReaperScreen.onOff("magnet", menu.getData(XpTankBlockEntity.DATA_MAGNET) != 0));
        areaButton.setMessage(ReaperScreen.onOff("show_area", menu.getData(XpTankBlockEntity.DATA_SHOW_AREA) != 0));
        powerButton.setMessage(ReaperScreen.onOff("power", menu.getData(XpTankBlockEntity.DATA_ENABLED) != 0));
        RedstoneMode mode = RedstoneMode.byIndex(menu.getData(XpTankBlockEntity.DATA_REDSTONE));
        redstoneButton.setMessage(Component.translatable("gui.grimsoul.redstone." + mode.id()));
    }

    private int storedXp() {
        return (menu.getData(XpTankBlockEntity.DATA_XP_LOW) & 0xFFFF) | ((menu.getData(XpTankBlockEntity.DATA_XP_HIGH) & 0xFFFF) << 16);
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
        float fill = menu.getData(XpTankBlockEntity.DATA_FILL) / 1000.0F;
        GuiStyle.bar(graphics, x + 12, y + 20, x + imageWidth - 12, y + 31, fill);
        Slot card = menu.slots.get(0);
        if (!card.hasItem()) {
            GuiStyle.ghost(graphics, new net.minecraft.world.item.ItemStack(com.xtri6.grimsoul.Registration.CARDS
                    .get(com.xtri6.grimsoul.item.UpgradeType.RANGE).get()), x + card.x, y + card.y);
        }
        graphics.fill(x + 8, y + 101, x + imageWidth - 8, y + 102, GuiStyle.PANEL_METAL);
        int light = menu.getData(XpTankBlockEntity.DATA_ENABLED) != 0 ? GuiStyle.ACCENT_GLOW : GuiStyle.PANEL_METAL;
        graphics.fill(x + imageWidth - 12, y + 7, x + imageWidth - 6, y + 13, light);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);
        GuiStyle.centered(graphics, font, Component.translatable("gui.grimsoul.xp_stored", GuiStyle.compact(storedXp())), imageWidth / 2, 22, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("gui.grimsoul.range_cards",
                menu.getData(XpTankBlockEntity.DATA_CARDS), CardMachineBlockEntity.MAX_RANGE_CARDS), 34, CardMachineMenu.TANK_CARD_Y, GuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.radius", menu.getData(XpTankBlockEntity.DATA_RADIUS)),
                34, CardMachineMenu.TANK_CARD_Y + 10, GuiStyle.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.magnet_area"), 12, 105, GuiStyle.TEXT_ACCENT, false);
        int[] values = {
                menu.getData(XpTankBlockEntity.DATA_AREA_X),
                menu.getData(XpTankBlockEntity.DATA_AREA_Y),
                menu.getData(XpTankBlockEntity.DATA_AREA_Z)};
        for (int axis = 0; axis < 3; axis++) {
            graphics.drawString(font, Component.translatable("gui.grimsoul.reach", AXES[axis]), 12, AREA_ROW_Y[axis] + 2, GuiStyle.TEXT_DIM, false);
            GuiStyle.centered(graphics, font, String.valueOf(values[axis]), 104, AREA_ROW_Y[axis] + 2, GuiStyle.TEXT);
        }
    }
}
