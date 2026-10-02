package com.xtri6.grimsoul.client;

import java.util.Locale;

import com.xtri6.grimsoul.blockentity.ReaperGeneratorBlockEntity;
import com.xtri6.grimsoul.machine.ItemSideMode;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.machine.RelativeSide;
import com.xtri6.grimsoul.menu.GeneratorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Reaper Generator GUI: fuel slots, energy bar, pass-through output, and two side maps (items and
 * energy) laid out like an unfolded block around the front face.
 */
public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private static final int SIDE_SIZE = 14;
    private static final int SIDE_PITCH = 15;
    private static final int SIDES_Y = 130;
    private static final int ITEM_MAP_X = 16;
    private static final int ENERGY_MAP_X = 98;
    private static final int BUTTON_Y = 120;

    private static final int COLOR_OFF = 0xFF3A3942;
    private static final int COLOR_IN = 0xFF3E6FD8;
    private static final int COLOR_OUT = 0xFFE08A2E;
    private static final int COLOR_BOTH = 0xFF8A4FE0;
    private static final int COLOR_ENERGY = 0xFFE0C23A;
    private static final int ENERGY_FILL = 0xFFE0C23A;
    private static final int ENERGY_SHINE = 0xFFFFE58A;

    private final SideButton[] itemButtons = new SideButton[RelativeSide.values().length];
    private final SideButton[] energyButtons = new SideButton[RelativeSide.values().length];
    private Button powerButton;
    private Button redstoneButton;
    private Button ejectButton;
    private Button passButton;

    public GeneratorScreen(GeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GeneratorMenu.WIDTH;
        this.inventoryLabelX = GuiStyle.invX(GeneratorMenu.WIDTH);
        this.titleLabelX = 12;
        this.imageHeight = 274;
        this.inventoryLabelY = GeneratorMenu.INV_Y - 11;
    }

    /** Where each side sits in the unfolded-block layout: column, row. */
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
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 26, topPos + 5));
        for (RelativeSide side : RelativeSide.values()) {
            int[] cell = layout(side);
            int dy = topPos + SIDES_Y + cell[1] * SIDE_PITCH;
            itemButtons[side.ordinal()] = addRenderableWidget(new SideButton(leftPos + ITEM_MAP_X + cell[0] * SIDE_PITCH, dy, side,
                    ReaperGeneratorBlockEntity.BTN_ITEM_SIDE + side.ordinal()));
            energyButtons[side.ordinal()] = addRenderableWidget(new SideButton(leftPos + ENERGY_MAP_X + cell[0] * SIDE_PITCH, dy, side,
                    ReaperGeneratorBlockEntity.BTN_ENERGY_SIDE + side.ordinal()));
        }
        powerButton = button(0, ReaperGeneratorBlockEntity.BTN_POWER, "power");
        redstoneButton = button(1, ReaperGeneratorBlockEntity.BTN_REDSTONE, "redstone");
        ejectButton = button(2, ReaperGeneratorBlockEntity.BTN_EJECT, "generator_eject");
        passButton = button(3, ReaperGeneratorBlockEntity.BTN_PASS, "generator_pass");
        updateLabels();
    }

    /** The four switches stack in a column to the right of the side maps. */
    private Button button(int row, int id, String key) {
        return addRenderableWidget(Button.builder(Component.empty(), b -> press(id))
                .bounds(leftPos + 176, topPos + BUTTON_Y + row * 16, 72, 14)
                .tooltip(Tooltip.create(Component.translatable("gui.grimsoul." + key + ".tooltip")))
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
        updateLabels();
    }

    private void updateLabels() {
        if (powerButton == null) {
            return;
        }
        powerButton.setMessage(ReaperScreen.onOff("power", menu.getData(ReaperGeneratorBlockEntity.DATA_ENABLED) != 0));
        RedstoneMode mode = RedstoneMode.byIndex(menu.getData(ReaperGeneratorBlockEntity.DATA_REDSTONE));
        redstoneButton.setMessage(Component.translatable("gui.grimsoul.redstone." + mode.id()));
        ejectButton.setMessage(ReaperScreen.onOff("eject", menu.getData(ReaperGeneratorBlockEntity.DATA_EJECT) != 0));
        passButton.setMessage(ReaperScreen.onOff("generator_pass", menu.getData(ReaperGeneratorBlockEntity.DATA_PASS) != 0));

        int itemSides = menu.getData(ReaperGeneratorBlockEntity.DATA_ITEM_SIDES);
        int energySides = menu.getData(ReaperGeneratorBlockEntity.DATA_ENERGY_SIDES);
        for (RelativeSide side : RelativeSide.values()) {
            Component sideName = Component.translatable("gui.grimsoul.side." + side.id());
            ItemSideMode itemMode = ReaperGeneratorBlockEntity.unpackItemSide(itemSides, side);
            SideButton itemButton = itemButtons[side.ordinal()];
            itemButton.color = switch (itemMode) {
                case OFF -> COLOR_OFF;
                case INPUT -> COLOR_IN;
                case OUTPUT -> COLOR_OUT;
                case BOTH -> COLOR_BOTH;
            };
            itemButton.setTooltip(Tooltip.create(Component.translatable("gui.grimsoul.side.items", sideName,
                    Component.translatable("gui.grimsoul.side.mode." + itemMode.id()))));

            boolean energyOut = ReaperGeneratorBlockEntity.unpackEnergySide(energySides, side);
            SideButton energyButton = energyButtons[side.ordinal()];
            energyButton.color = energyOut ? COLOR_ENERGY : COLOR_OFF;
            energyButton.setTooltip(Tooltip.create(Component.translatable("gui.grimsoul.side.energy", sideName,
                    Component.translatable(energyOut ? "gui.grimsoul.side.mode.out" : "gui.grimsoul.side.mode.off"))));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + 78;
        int y = topPos + 22;
        if (mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 52) {
            graphics.renderTooltip(font, Component.translatable("gui.grimsoul.generator.energy_tooltip",
                    String.format(Locale.ROOT, "%,d", menu.getSplit(ReaperGeneratorBlockEntity.DATA_ENERGY_LO)),
                    String.format(Locale.ROOT, "%,d", menu.getSplit(ReaperGeneratorBlockEntity.DATA_CAPACITY_LO))), mouseX, mouseY);
            return;
        }
        Slot hovered = this.hoveredSlot;
        if (hovered != null && !hovered.hasItem() && menu.getCarried().isEmpty()) {
            int index = menu.slots.indexOf(hovered);
            if (index < GeneratorMenu.OUTPUT_START) {
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.generator.fuel_slot"), mouseX, mouseY);
                return;
            }
            if (index < GeneratorMenu.PLAYER_START) {
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.generator.output_slot"), mouseX, mouseY);
                return;
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        GuiStyle.panel(graphics, x, y, x + imageWidth, y + imageHeight);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int bg = i < GeneratorMenu.OUTPUT_START ? GuiStyle.FILTER_BG : GuiStyle.SLOT_BG;
            GuiStyle.slot(graphics, x + slot.x, y + slot.y, bg);
        }

        // Vertical energy bar
        int energy = menu.getSplit(ReaperGeneratorBlockEntity.DATA_ENERGY_LO);
        int capacity = Math.max(1, menu.getSplit(ReaperGeneratorBlockEntity.DATA_CAPACITY_LO));
        int bx1 = x + 78, by1 = y + 22, bx2 = x + 88, by2 = y + 74;
        graphics.fill(bx1 - 1, by1 - 1, bx2 + 1, by2 + 1, GuiStyle.SLOT_BORDER);
        graphics.fill(bx1, by1, bx2, by2, GuiStyle.BAR_BG);
        int filled = Math.round((by2 - by1) * Math.min(1.0F, energy / (float) capacity));
        if (filled > 0) {
            graphics.fill(bx1, by2 - filled, bx2, by2, ENERGY_FILL);
            graphics.fill(bx1, by2 - filled, bx1 + 2, by2, ENERGY_SHINE);
        }

        // Fuel burn bar
        int burn = menu.getData(ReaperGeneratorBlockEntity.DATA_BURN);
        int burnTotal = Math.max(1, menu.getData(ReaperGeneratorBlockEntity.DATA_BURN_TOTAL));
        GuiStyle.bar(graphics, x + 98, y + 68, x + 248, y + 74, burn / (float) burnTotal);

        int light = menu.getData(ReaperGeneratorBlockEntity.DATA_RATE_LO) != 0 || menu.getData(ReaperGeneratorBlockEntity.DATA_RATE_HI) != 0
                ? GuiStyle.ACCENT_GLOW : GuiStyle.PANEL_METAL;
        graphics.fill(x + imageWidth - 12, y + 7, x + imageWidth - 6, y + 13, light);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);

        int energy = menu.getSplit(ReaperGeneratorBlockEntity.DATA_ENERGY_LO);
        int capacity = menu.getSplit(ReaperGeneratorBlockEntity.DATA_CAPACITY_LO);
        int rate = menu.getSplit(ReaperGeneratorBlockEntity.DATA_RATE_LO);
        int burn = menu.getData(ReaperGeneratorBlockEntity.DATA_BURN);

        graphics.drawString(font, Component.translatable("gui.grimsoul.generator.energy"), 98, 22, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, GuiStyle.compact(energy) + " / " + GuiStyle.compact(capacity) + " FE", 98, 32, GuiStyle.TEXT, false);
        Component status = rate > 0
                ? Component.translatable("gui.grimsoul.generator.making", GuiStyle.compact(rate))
                : Component.translatable(burn > 0 ? "gui.grimsoul.generator.full" : "gui.grimsoul.generator.idle");
        graphics.drawString(font, status, 98, 44, rate > 0 ? GuiStyle.TEXT_OK : GuiStyle.TEXT_WARN, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.generator.burn",
                String.format(Locale.ROOT, "%.1f", burn / 20.0)), 98, 56, GuiStyle.TEXT_DIM, false);

        graphics.drawString(font, Component.translatable("gui.grimsoul.generator.output"), GeneratorMenu.OUTPUT_X, 86, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.generator.item_sides"), ITEM_MAP_X, 120, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.generator.energy_sides"), ENERGY_MAP_X, 120, GuiStyle.TEXT_ACCENT, false);
    }

    /** A small colored square for one side of the machine, labeled with the side's initial. */
    private class SideButton extends Button {
        private final RelativeSide side;
        int color = COLOR_OFF;

        SideButton(int x, int y, RelativeSide side, int id) {
            super(x, y, SIDE_SIZE, SIDE_SIZE, Component.empty(), b -> press(id), DEFAULT_NARRATION);
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
