package com.xtri6.grimsoul.client;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.ReaperBlockEntity;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.machine.TargetMode;
import com.xtri6.grimsoul.menu.ReaperMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Reaper Block GUI. Left: machine storage and player inventory. Right: upgrade cards, filter,
 * work-area controls and machine settings.
 */
public class ReaperScreen extends AbstractContainerScreen<ReaperMenu> {
    private static final int MAIN_W = 176;
    private static final int MAIN_H = 222;
    private static final int SIDE_LEFT = 180;
    private static final int TOTAL_W = 360;
    private static final int TOTAL_H = 282;

    private static final int AREA_HEADER_Y = 128;
    private static final int[] AREA_ROW_Y = {138, 152, 166};
    private static final int ROW_1 = 182;
    private static final int ROW_2 = 198;
    private static final int ROW_3 = 214;
    private static final int ROW_4 = 230;
    private static final int INFO_Y = 248;

    private Button whitelistButton;
    private Button voidButton;
    private Button voidWhenFullButton;
    private Button showAreaButton;
    private Button autoAreaButton;
    private Button ejectButton;
    private Button magnetButton;
    private Button powerButton;
    private Button redstoneButton;
    private Button targetButton;

    public ReaperScreen(ReaperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TOTAL_W;
        this.imageHeight = TOTAL_H;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 16, topPos + 5));
        int x = leftPos;
        int y = topPos;

        whitelistButton = button(x + 256, y + ReaperMenu.FILTER_Y, 92, ReaperBlockEntity.BTN_WHITELIST, "whitelist");
        voidButton = button(x + 256, y + ReaperMenu.FILTER_Y + 18, 92, ReaperBlockEntity.BTN_VOID, "void");
        voidWhenFullButton = button(x + 256, y + ReaperMenu.FILTER_Y + 36, 92, ReaperBlockEntity.BTN_VOID_WHEN_FULL, "void_full");

        String[] axes = {"X", "Y", "Z"};
        for (int axis = 0; axis < 3; axis++) {
            int rowY = y + AREA_ROW_Y[axis];
            Tooltip offsetTip = Tooltip.create(Component.translatable("gui.grimsoul.offset.tooltip", axes[axis]));
            Tooltip sizeTip = Tooltip.create(Component.translatable("gui.grimsoul.size.tooltip", axes[axis]));
            smallButton(x + 206, rowY, "-", ReaperBlockEntity.BTN_OFFSET_BASE + axis * 2, offsetTip);
            smallButton(x + 244, rowY, "+", ReaperBlockEntity.BTN_OFFSET_BASE + axis * 2 + 1, offsetTip);
            smallButton(x + 284, rowY, "-", ReaperBlockEntity.BTN_SIZE_BASE + axis * 2, sizeTip);
            smallButton(x + 322, rowY, "+", ReaperBlockEntity.BTN_SIZE_BASE + axis * 2 + 1, sizeTip);
        }

        showAreaButton = button(x + 188, y + ROW_1, 78, ReaperBlockEntity.BTN_SHOW_AREA, "show_area");
        autoAreaButton = button(x + 270, y + ROW_1, 78, ReaperBlockEntity.BTN_AUTO_AREA, "auto_area");
        magnetButton = button(x + 188, y + ROW_2, 78, ReaperBlockEntity.BTN_MAGNET, "reaper_magnet");
        ejectButton = button(x + 270, y + ROW_2, 78, ReaperBlockEntity.BTN_AUTO_EJECT, "eject");
        Button takeXp = button(x + 300, y + INFO_Y - 3, 48, ReaperBlockEntity.BTN_TAKE_XP, "take_xp");
        takeXp.setMessage(Component.translatable("gui.grimsoul.take_xp_short"));
        powerButton = button(x + 188, y + ROW_3, 78, ReaperBlockEntity.BTN_POWER, "power");
        redstoneButton = button(x + 270, y + ROW_3, 78, ReaperBlockEntity.BTN_REDSTONE, "redstone");
        targetButton = button(x + 188, y + ROW_4, 160, ReaperBlockEntity.BTN_TARGET, "target");

        updateButtonLabels();
    }

    private Button button(int x, int y, int width, int id, String tooltipKey) {
        return addRenderableWidget(Button.builder(Component.empty(), b -> press(id))
                .bounds(x, y, width, 14)
                .tooltip(Tooltip.create(Component.translatable("gui.grimsoul." + tooltipKey + ".tooltip")))
                .build());
    }

    private void smallButton(int x, int y, String label, int id, Tooltip tooltip) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> press(id))
                .bounds(x, y, 12, 12)
                .tooltip(tooltip)
                .build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonLabels();
    }

    private boolean flag(int index) {
        return menu.getData(index) != 0;
    }

    static Component onOff(String key, boolean on) {
        return Component.translatable("gui.grimsoul." + key, Component.translatable(on ? "gui.grimsoul.on" : "gui.grimsoul.off"));
    }

    private void updateButtonLabels() {
        if (whitelistButton == null) {
            return;
        }
        whitelistButton.setMessage(Component.translatable(flag(ReaperBlockEntity.DATA_WHITELIST) ? "gui.grimsoul.whitelist" : "gui.grimsoul.blacklist"));
        voidButton.setMessage(!flag(ReaperBlockEntity.DATA_HAS_VOID_CARD)
                ? Component.translatable("gui.grimsoul.void.no_card")
                : onOff("void", flag(ReaperBlockEntity.DATA_VOID)));
        voidWhenFullButton.setMessage(Component.translatable(flag(ReaperBlockEntity.DATA_VOID_WHEN_FULL) ? "gui.grimsoul.void_full.on" : "gui.grimsoul.void_full.off"));
        showAreaButton.setMessage(onOff("show_area", flag(ReaperBlockEntity.DATA_SHOW_AREA)));
        autoAreaButton.setMessage(onOff("auto_area", flag(ReaperBlockEntity.DATA_AUTO_AREA)));
        ejectButton.setMessage(onOff("eject", flag(ReaperBlockEntity.DATA_AUTO_EJECT)));
        magnetButton.setMessage(onOff("magnet", flag(ReaperBlockEntity.DATA_MAGNET)));
        powerButton.setMessage(onOff("power", flag(ReaperBlockEntity.DATA_ENABLED)));
        RedstoneMode redstone = RedstoneMode.byIndex(menu.getData(ReaperBlockEntity.DATA_REDSTONE));
        redstoneButton.setMessage(Component.translatable("gui.grimsoul.redstone." + redstone.id()));
        TargetMode target = TargetMode.byIndex(menu.getData(ReaperBlockEntity.DATA_TARGET));
        targetButton.setMessage(Component.translatable("gui.grimsoul.target." + target.id()));
    }

    private ItemStack cardIcon(int index) {
        UpgradeType type = ReaperBlockEntity.CARD_SLOTS[index];
        return new ItemStack(Registration.CARDS.get(type).get());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Slot hovered = this.hoveredSlot;
        if (hovered != null && !hovered.hasItem() && menu.getCarried().isEmpty()) {
            int index = menu.slots.indexOf(hovered);
            if (index == ReaperMenu.CORE_SLOT) {
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.reaper_core_slot"), mouseX, mouseY);
                return;
            }
            if (index >= ReaperMenu.UPGRADE_START && index < ReaperMenu.FILTER_START) {
                UpgradeType type = ReaperBlockEntity.CARD_SLOTS[index - ReaperMenu.UPGRADE_START];
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.card_slot",
                        cardIcon(index - ReaperMenu.UPGRADE_START).getHoverName(), type.maxCards()), mouseX, mouseY);
                return;
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        GuiStyle.panel(graphics, x, y, x + MAIN_W, y + MAIN_H);
        GuiStyle.panel(graphics, x + SIDE_LEFT, y, x + TOTAL_W, y + TOTAL_H);

        int activeSlots = menu.getActiveSlots();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = x + slot.x;
            int sy = y + slot.y;
            boolean filter = i >= ReaperMenu.FILTER_START && i < ReaperMenu.CORE_SLOT;
            GuiStyle.slot(graphics, sx, sy, filter || i == ReaperMenu.CORE_SLOT ? GuiStyle.FILTER_BG : GuiStyle.SLOT_BG);
            if (i == ReaperMenu.CORE_SLOT && !slot.hasItem()) {
                GuiStyle.ghost(graphics, new ItemStack(Registration.DATA_CORE.get()), sx, sy);
            }
            if (i < ReaperMenu.UPGRADE_START && i >= activeSlots) {
                graphics.fill(sx, sy, sx + 16, sy + 16, GuiStyle.LOCKED);
            }
            // Empty card slots show a faded picture of the card that belongs there.
            if (i >= ReaperMenu.UPGRADE_START && i < ReaperMenu.FILTER_START && !slot.hasItem()) {
                GuiStyle.ghost(graphics, cardIcon(i - ReaperMenu.UPGRADE_START), sx, sy);
            }
        }
        // Status light next to the title: purple when switched on, gray when off.
        int light = flag(ReaperBlockEntity.DATA_ENABLED) ? GuiStyle.ACCENT_GLOW : GuiStyle.PANEL_METAL;
        graphics.fill(x + MAIN_W - 12, y + 7, x + MAIN_W - 6, y + 13, light);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);

        int side = ReaperMenu.SIDE_X;
        graphics.drawString(font, Component.translatable("gui.grimsoul.upgrades"), side, 7, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.filter"), side, ReaperMenu.FILTER_Y - 10, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.offset"), 208, AREA_HEADER_Y, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.size"), 294, AREA_HEADER_Y, GuiStyle.TEXT_ACCENT, false);

        String[] axes = {"X", "Y", "Z"};
        int[] offsets = {
                menu.getData(ReaperBlockEntity.DATA_OFF_X),
                menu.getData(ReaperBlockEntity.DATA_OFF_Y),
                menu.getData(ReaperBlockEntity.DATA_OFF_Z)};
        int[] sizes = {
                menu.getData(ReaperBlockEntity.DATA_SIZE_X),
                menu.getData(ReaperBlockEntity.DATA_SIZE_Y),
                menu.getData(ReaperBlockEntity.DATA_SIZE_Z)};
        for (int axis = 0; axis < 3; axis++) {
            int rowY = AREA_ROW_Y[axis] + 2;
            graphics.drawString(font, axes[axis], side, rowY, GuiStyle.TEXT_DIM, false);
            GuiStyle.centered(graphics, font, String.valueOf((short) offsets[axis]), 231, rowY, GuiStyle.TEXT);
            GuiStyle.centered(graphics, font, String.valueOf(sizes[axis]), 309, rowY, GuiStyle.TEXT);
        }

        graphics.drawString(font, Component.translatable("gui.grimsoul.xp", GuiStyle.compact(menu.getStoredXp())), side, INFO_Y, GuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.limits",
                menu.getData(ReaperBlockEntity.DATA_MAX_SIZE), menu.getData(ReaperBlockEntity.DATA_MAX_OFFSET)), side, INFO_Y + 10, GuiStyle.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.slots", menu.getActiveSlots(), ReaperBlockEntity.MAX_SLOTS), side, INFO_Y + 20, GuiStyle.TEXT_DIM, false);
    }
}
