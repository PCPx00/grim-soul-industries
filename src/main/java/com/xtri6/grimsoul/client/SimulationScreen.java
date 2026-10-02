package com.xtri6.grimsoul.client;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.SimulationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Simulation Chamber GUI: core slot, cards, power and progress bars, output, and switches. */
public class SimulationScreen extends AbstractContainerScreen<SimulationMenu> {
    private static final int BAR_X1 = 128, BAR_X2 = 248;
    private static final int ENERGY_Y = 54, PROGRESS_Y = 66;
    private static final int BUTTON_Y = 154;

    private Button powerButton;
    private Button redstoneButton;
    private Button ejectButton;

    public SimulationScreen(SimulationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SimulationMenu.WIDTH;
        this.inventoryLabelX = GuiStyle.invX(SimulationMenu.WIDTH);
        this.titleLabelX = 12;
        this.imageHeight = 280;
        this.inventoryLabelY = SimulationMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 26, topPos + 5));
        powerButton = button(12, 0, 115, SimulationChamberBlockEntity.BTN_POWER, "power");
        redstoneButton = button(133, 0, 115, SimulationChamberBlockEntity.BTN_REDSTONE, "redstone");
        ejectButton = button(12, 1, 115, SimulationChamberBlockEntity.BTN_EJECT, "sim_eject");
        Button takeXp = button(133, 1, 115, SimulationChamberBlockEntity.BTN_TAKE_XP, "sim_take_xp");
        takeXp.setMessage(Component.translatable("gui.grimsoul.take_xp"));
        updateLabels();
    }

    private Button button(int x, int row, int width, int id, String key) {
        return addRenderableWidget(Button.builder(Component.empty(), b -> press(id))
                .bounds(leftPos + x, topPos + BUTTON_Y + row * 17, width, 14)
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
        powerButton.setMessage(ReaperScreen.onOff("power", menu.getData(SimulationChamberBlockEntity.DATA_ENABLED) != 0));
        RedstoneMode mode = RedstoneMode.byIndex(menu.getData(SimulationChamberBlockEntity.DATA_REDSTONE));
        redstoneButton.setMessage(Component.translatable("gui.grimsoul.redstone." + mode.id()));
        ejectButton.setMessage(ReaperScreen.onOff("eject", menu.getData(SimulationChamberBlockEntity.DATA_EJECT) != 0));
    }

    private ItemStack cardIcon(int index) {
        return new ItemStack(Registration.CARDS.get(SimulationChamberBlockEntity.CARD_SLOTS[index]).get());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= BAR_X1 && mx < BAR_X2 && my >= ENERGY_Y - 1 && my < ENERGY_Y + 7) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.grimsoul.spawner.energy",
                            String.format(Locale.ROOT, "%,d", menu.getSplit(SimulationChamberBlockEntity.DATA_ENERGY_LO)),
                            String.format(Locale.ROOT, "%,d", menu.getSplit(SimulationChamberBlockEntity.DATA_CAPACITY_LO))),
                    Component.translatable("gui.grimsoul.sim.energy_use",
                            String.format(Locale.ROOT, "%,d", menu.getSplit(SimulationChamberBlockEntity.DATA_COST_LO)))),
                    mouseX, mouseY);
            return;
        }
        Slot hovered = this.hoveredSlot;
        if (hovered != null && !hovered.hasItem() && menu.getCarried().isEmpty()) {
            int index = menu.slots.indexOf(hovered);
            if (index == 0) {
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.sim.core_slot"), mouseX, mouseY);
                return;
            }
            if (index >= SimulationMenu.CARD_START && index < SimulationMenu.OUTPUT_START) {
                int card = index - SimulationMenu.CARD_START;
                UpgradeType type = SimulationChamberBlockEntity.CARD_SLOTS[card];
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.card_slot", cardIcon(card).getHoverName(), type.maxCards()), mouseX, mouseY);
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
            int sx = x + slot.x;
            int sy = y + slot.y;
            GuiStyle.slot(graphics, sx, sy, i == 0 ? GuiStyle.FILTER_BG : GuiStyle.SLOT_BG);
            if (!slot.hasItem()) {
                ItemStack ghost = ItemStack.EMPTY;
                if (i == 0) {
                    ghost = new ItemStack(Registration.DATA_CORE.get());
                } else if (i < SimulationMenu.OUTPUT_START) {
                    ghost = cardIcon(i - SimulationMenu.CARD_START);
                }
                if (!ghost.isEmpty()) {
                    GuiStyle.ghost(graphics, ghost, sx, sy);
                }
            }
        }
        // Energy (yellow) and run progress (purple)
        int energy = menu.getSplit(SimulationChamberBlockEntity.DATA_ENERGY_LO);
        int capacity = Math.max(1, menu.getSplit(SimulationChamberBlockEntity.DATA_CAPACITY_LO));
        int ex1 = x + BAR_X1, ex2 = x + BAR_X2, ey1 = y + ENERGY_Y, ey2 = y + ENERGY_Y + 6;
        graphics.fill(ex1 - 1, ey1 - 1, ex2 + 1, ey2 + 1, GuiStyle.SLOT_BORDER);
        graphics.fill(ex1, ey1, ex2, ey2, GuiStyle.BAR_BG);
        int filled = Math.round((ex2 - ex1) * Math.min(1.0F, energy / (float) capacity));
        if (filled > 0) {
            graphics.fill(ex1, ey1, ex1 + filled, ey2, 0xFFE0C23A);
            graphics.fill(ex1, ey1, ex1 + filled, ey1 + 2, 0xFFFFE58A);
        }
        int interval = Math.max(1, menu.getData(SimulationChamberBlockEntity.DATA_INTERVAL));
        GuiStyle.bar(graphics, x + BAR_X1, y + PROGRESS_Y, x + BAR_X2, y + PROGRESS_Y + 6,
                menu.getData(SimulationChamberBlockEntity.DATA_PROGRESS) / (float) interval);

        boolean running = menu.getData(SimulationChamberBlockEntity.DATA_STATUS) == SimulationChamberBlockEntity.STATUS_RUNNING;
        graphics.fill(x + imageWidth - 12, y + 7, x + imageWidth - 6, y + 13, running ? GuiStyle.ACCENT_GLOW : GuiStyle.PANEL_METAL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);

        ItemStack coreStack = menu.slots.get(0).getItem();
        Optional<EntityType<?>> mob = DataCoreItem.getMob(coreStack);
        Component mobName = mob.<Component>map(EntityType::getDescription).orElse(Component.translatable("gui.grimsoul.spawner.no_mob"));
        graphics.drawString(font, Component.translatable("gui.grimsoul.spawner.mob", mobName), 34, 22, GuiStyle.TEXT, false);
        if (mob.isPresent()) {
            graphics.drawString(font, DataCoreItem.tierName(DataCoreItem.getTier(coreStack)).getString(), 34, 32, GuiStyle.TEXT_ACCENT, false);
        }

        int status = menu.getData(SimulationChamberBlockEntity.DATA_STATUS);
        int color = status == SimulationChamberBlockEntity.STATUS_RUNNING ? GuiStyle.TEXT_OK : GuiStyle.TEXT_WARN;
        graphics.drawString(font, Component.translatable("gui.grimsoul.sim.status." + status), 12, 82, color, false);

        graphics.drawString(font, Component.translatable("gui.grimsoul.upgrades"), 12, 45, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, "FE", 106, ENERGY_Y - 1, GuiStyle.TEXT_ENERGY, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.sim.run"), 106, PROGRESS_Y - 1, GuiStyle.TEXT_DIM, false);

        int interval = menu.getData(SimulationChamberBlockEntity.DATA_INTERVAL);
        graphics.drawString(font, Component.translatable("gui.grimsoul.sim.info",
                GuiStyle.compact(menu.getSplit(SimulationChamberBlockEntity.DATA_COST_LO)),
                String.format(Locale.ROOT, "%.1f", interval / 20.0),
                menu.getData(SimulationChamberBlockEntity.DATA_CHANCE)), 12, 92, GuiStyle.TEXT_DIM, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.output"), SimulationMenu.OUTPUT_X, 103, GuiStyle.TEXT_ACCENT, false);
        Component stats = Component.translatable("gui.grimsoul.sim.stats",
                GuiStyle.compact(menu.getSplit(SimulationChamberBlockEntity.DATA_XP_LO)),
                GuiStyle.compact(menu.getSplit(SimulationChamberBlockEntity.DATA_RUNS_LO)));
        graphics.drawString(font, stats, SimulationMenu.OUTPUT_X + 162 - font.width(stats), 103, GuiStyle.TEXT_DIM, false);
    }
}
