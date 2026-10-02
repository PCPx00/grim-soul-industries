package com.xtri6.grimsoul.client;

import java.util.Locale;

import com.xtri6.grimsoul.blockentity.EnergyCellBlockEntity;
import com.xtri6.grimsoul.machine.RelativeSide;
import com.xtri6.grimsoul.menu.CellMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Energy Cell GUI: a big charge gauge, in/out rates, and a side map (blue in, orange out, gray off). */
public class CellScreen extends AbstractContainerScreen<CellMenu> {
    private static final int COLOR_IN = 0xFF3E6FD8;
    private static final int COLOR_OUT = 0xFFE08A2E;
    private static final int COLOR_OFF = 0xFF3A3942;
    private static final int MAP_X = 156, MAP_Y = 60, PITCH = 15, SIZE = 14;
    private static final int BAR_X1 = 12, BAR_X2 = 32, BAR_Y1 = 20, BAR_Y2 = 100;

    private final SideButton[] sideButtons = new SideButton[RelativeSide.values().length];

    public CellScreen(CellMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 232;
        this.imageHeight = 114;
    }

    private static int[] layout(RelativeSide side) {
        return switch (side) {
            case TOP -> new int[] {1, 0};
            case LEFT -> new int[] {0, 1};
            case FRONT -> new int[] {1, 1};
            case RIGHT -> new int[] {2, 1};
            case BACK -> new int[] {3, 1};
            case BOTTOM -> new int[] {1, 2};
        };
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 16, topPos + 5));
        for (RelativeSide side : RelativeSide.values()) {
            int[] cell = layout(side);
            sideButtons[side.ordinal()] = addRenderableWidget(new SideButton(leftPos + MAP_X + cell[0] * PITCH,
                    topPos + MAP_Y + cell[1] * PITCH, side, EnergyCellBlockEntity.BTN_SIDE + side.ordinal()));
        }
        updateLabels();
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
        int packed = menu.getData(EnergyCellBlockEntity.DATA_SIDES);
        for (RelativeSide side : RelativeSide.values()) {
            int mode = EnergyCellBlockEntity.unpackSide(packed, side);
            SideButton button = sideButtons[side.ordinal()];
            if (button == null) {
                continue;
            }
            button.color = mode == EnergyCellBlockEntity.MODE_IN ? COLOR_IN : mode == EnergyCellBlockEntity.MODE_OUT ? COLOR_OUT : COLOR_OFF;
            String modeKey = mode == EnergyCellBlockEntity.MODE_IN ? "in" : mode == EnergyCellBlockEntity.MODE_OUT ? "out" : "off";
            button.setTooltip(Tooltip.create(Component.translatable("gui.grimsoul.cell.side",
                    Component.translatable("gui.grimsoul.side." + side.id()),
                    Component.translatable("gui.grimsoul.side.mode." + modeKey))));
        }
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
        int energy = menu.getSplit(EnergyCellBlockEntity.DATA_ENERGY_LO);
        int capacity = Math.max(1, menu.getSplit(EnergyCellBlockEntity.DATA_CAPACITY_LO));
        graphics.fill(x + BAR_X1 - 1, y + BAR_Y1 - 1, x + BAR_X2 + 1, y + BAR_Y2 + 1, GuiStyle.SLOT_BORDER);
        graphics.fill(x + BAR_X1, y + BAR_Y1, x + BAR_X2, y + BAR_Y2, GuiStyle.BAR_BG);
        int filled = Math.round((BAR_Y2 - BAR_Y1) * Math.min(1.0F, energy / (float) capacity));
        if (filled > 0) {
            graphics.fill(x + BAR_X1, y + BAR_Y2 - filled, x + BAR_X2, y + BAR_Y2, 0xFFE0C23A);
            graphics.fill(x + BAR_X1, y + BAR_Y2 - filled, x + BAR_X1 + 3, y + BAR_Y2, 0xFFFFE58A);
        }
        for (int i = 1; i < 8; i++) {
            int ty = y + BAR_Y1 + (BAR_Y2 - BAR_Y1) * i / 8;
            graphics.fill(x + BAR_X2 - 4, ty, x + BAR_X2, ty + 1, 0x80000000);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        int energy = menu.getSplit(EnergyCellBlockEntity.DATA_ENERGY_LO);
        int capacity = menu.getSplit(EnergyCellBlockEntity.DATA_CAPACITY_LO);
        int percent = capacity <= 0 ? 0 : (int) ((long) energy * 100 / capacity);
        graphics.drawString(font, String.format(Locale.ROOT, "%,d FE", energy), 42, 20, GuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.cell.of", String.format(Locale.ROOT, "%,d", capacity), percent), 42, 30, GuiStyle.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.cell.in",
                GuiStyle.compact(menu.getSplit(EnergyCellBlockEntity.DATA_IN_LO))), 42, 48, GuiStyle.TEXT_IN, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.cell.out",
                GuiStyle.compact(menu.getSplit(EnergyCellBlockEntity.DATA_OUT_LO))), 42, 58, GuiStyle.TEXT_OUT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.cell.sides"), MAP_X, MAP_Y - 10, GuiStyle.TEXT_ACCENT, false);
    }

    private class SideButton extends Button {
        private final RelativeSide side;
        int color = COLOR_OFF;

        SideButton(int x, int y, RelativeSide side, int id) {
            super(x, y, SIZE, SIZE, Component.empty(), b -> press(id), DEFAULT_NARRATION);
            this.side = side;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x1 = getX(), y1 = getY(), x2 = x1 + width, y2 = y1 + height;
            graphics.fill(x1, y1, x2, y2, isHoveredOrFocused() ? GuiStyle.TEXT : GuiStyle.PANEL_EDGE);
            graphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, color);
            graphics.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0x40FFFFFF);
            String label = Component.translatable("gui.grimsoul.side.short." + side.id()).getString();
            graphics.drawString(font, label, x1 + (width - font.width(label)) / 2 + 1, y1 + 3, 0xFFFFFFFF, true);
        }
    }
}
