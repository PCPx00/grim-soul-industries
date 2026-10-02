package com.xtri6.grimsoul.client;

import java.util.Locale;
import java.util.Optional;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;
import com.xtri6.grimsoul.item.ReapersLanternItem;
import com.xtri6.grimsoul.item.UpgradeType;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.menu.SpawnerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Essence Spawner GUI: lantern slot, one slot per spawner card, status, and switches. */
public class SpawnerScreen extends AbstractContainerScreen<SpawnerMenu> {

    private Button areaButton;
    private Button powerButton;
    private Button redstoneButton;

    public SpawnerScreen(SpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = SpawnerMenu.WIDTH;
        this.inventoryLabelX = GuiStyle.invX(SpawnerMenu.WIDTH);
        this.titleLabelX = 12;
        this.imageHeight = 228;
        this.inventoryLabelY = SpawnerMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.themeButton(leftPos + imageWidth - 26, topPos + 5));
        int x = leftPos;
        int y = topPos;
        areaButton = button(x + 12, y + 102, 72, EssenceSpawnerBlockEntity.BTN_SHOW_AREA, "spawner_area");
        powerButton = button(x + 89, y + 102, 72, EssenceSpawnerBlockEntity.BTN_POWER, "power");
        redstoneButton = button(x + 166, y + 102, 72, EssenceSpawnerBlockEntity.BTN_REDSTONE, "redstone");
        updateLabels();
    }

    private Button button(int x, int y, int width, int id, String key) {
        return addRenderableWidget(Button.builder(Component.empty(), b -> press(id))
                .bounds(x, y, width, 14)
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
        if (areaButton == null) {
            return;
        }
        areaButton.setMessage(ReaperScreen.onOff("show_area", menu.getData(EssenceSpawnerBlockEntity.DATA_SHOW_AREA) != 0));
        powerButton.setMessage(ReaperScreen.onOff("power", menu.getData(EssenceSpawnerBlockEntity.DATA_ENABLED) != 0));
        RedstoneMode mode = RedstoneMode.byIndex(menu.getData(EssenceSpawnerBlockEntity.DATA_REDSTONE));
        redstoneButton.setMessage(Component.translatable("gui.grimsoul.redstone." + mode.id()));
    }

    private ItemStack cardIcon(int index) {
        return new ItemStack(Registration.CARDS.get(EssenceSpawnerBlockEntity.CARD_SLOTS[index]).get());
    }

    private static final int ENERGY_X1 = 30, ENERGY_X2 = 238, ENERGY_Y1 = 125, ENERGY_Y2 = 131;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + ENERGY_X1 && mouseX < leftPos + ENERGY_X2 && mouseY >= topPos + ENERGY_Y1 - 1 && mouseY < topPos + ENERGY_Y2 + 1) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.grimsoul.spawner.energy",
                            String.format(Locale.ROOT, "%,d", menu.getSplit(EssenceSpawnerBlockEntity.DATA_ENERGY_LO)),
                            String.format(Locale.ROOT, "%,d", menu.getSplit(EssenceSpawnerBlockEntity.DATA_CAPACITY_LO))),
                    Component.translatable("gui.grimsoul.spawner.energy_use",
                            String.format(Locale.ROOT, "%,d", menu.getSplit(EssenceSpawnerBlockEntity.DATA_COST_LO)))),
                    mouseX, mouseY);
            return;
        }
        Slot hovered = this.hoveredSlot;
        if (hovered != null && !hovered.hasItem() && menu.getCarried().isEmpty()) {
            int index = menu.slots.indexOf(hovered);
            if (index >= SpawnerMenu.CARD_START && index < SpawnerMenu.PLAYER_START) {
                int card = index - SpawnerMenu.CARD_START;
                UpgradeType type = EssenceSpawnerBlockEntity.CARD_SLOTS[card];
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.card_slot", cardIcon(card).getHoverName(), type.maxCards()), mouseX, mouseY);
                return;
            }
            if (index == 0) {
                graphics.renderTooltip(font, Component.translatable("gui.grimsoul.lantern_slot"), mouseX, mouseY);
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
                    ghost = new ItemStack(Registration.REAPERS_LANTERN.get());
                } else if (i < SpawnerMenu.PLAYER_START) {
                    ghost = cardIcon(i - SpawnerMenu.CARD_START);
                }
                if (!ghost.isEmpty()) {
                    GuiStyle.ghost(graphics, ghost, sx, sy);
                }
            }
        }
        // Progress to the next spawn
        int interval = Math.max(1, menu.getData(EssenceSpawnerBlockEntity.DATA_INTERVAL));
        float progress = menu.getData(EssenceSpawnerBlockEntity.DATA_PROGRESS) / (float) interval;
        GuiStyle.bar(graphics, x + 166, y + 58, x + 238, y + 66, progress);
        // Energy bar (yellow, like the generator's)
        int energy = menu.getSplit(EssenceSpawnerBlockEntity.DATA_ENERGY_LO);
        int capacity = Math.max(1, menu.getSplit(EssenceSpawnerBlockEntity.DATA_CAPACITY_LO));
        int ex1 = x + ENERGY_X1, ex2 = x + ENERGY_X2, ey1 = y + ENERGY_Y1, ey2 = y + ENERGY_Y2;
        graphics.fill(ex1 - 1, ey1 - 1, ex2 + 1, ey2 + 1, GuiStyle.SLOT_BORDER);
        graphics.fill(ex1, ey1, ex2, ey2, GuiStyle.BAR_BG);
        int filled = Math.round((ex2 - ex1) * Math.min(1.0F, energy / (float) capacity));
        if (filled > 0) {
            graphics.fill(ex1, ey1, ex1 + filled, ey2, 0xFFE0C23A);
            graphics.fill(ex1, ey1, ex1 + filled, ey1 + 2, 0xFFFFE58A);
        }
        int light = menu.getData(EssenceSpawnerBlockEntity.DATA_ENABLED) != 0 ? GuiStyle.ACCENT_GLOW : GuiStyle.PANEL_METAL;
        graphics.fill(x + imageWidth - 12, y + 7, x + imageWidth - 6, y + 13, light);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiStyle.TEXT_DIM, false);

        Optional<EntityType<?>> mob = ReapersLanternItem.getCaptured(menu.slots.get(0).getItem());
        Component mobName = mob.<Component>map(EntityType::getDescription)
                .orElse(Component.translatable("gui.grimsoul.spawner.no_mob"));
        graphics.drawString(font, Component.translatable("gui.grimsoul.spawner.mob", mobName), 34, 22, GuiStyle.TEXT, false);

        int status = menu.getData(EssenceSpawnerBlockEntity.DATA_STATUS);
        int color = status == EssenceSpawnerBlockEntity.STATUS_RUNNING ? GuiStyle.TEXT_OK : GuiStyle.TEXT_WARN;
        graphics.drawString(font, Component.translatable("gui.grimsoul.spawner.status." + status), 34, 32, color, false);

        graphics.drawString(font, "FE", 12, ENERGY_Y1 - 1, GuiStyle.TEXT_ENERGY, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.upgrades"), 12, 45, GuiStyle.TEXT_ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.spawner.next"), 166, 47, GuiStyle.TEXT_DIM, false);

        int interval = menu.getData(EssenceSpawnerBlockEntity.DATA_INTERVAL);
        String seconds = String.format(Locale.ROOT, "%.1f", interval / 20.0);
        graphics.drawString(font, Component.translatable("gui.grimsoul.spawner.rate",
                menu.getData(EssenceSpawnerBlockEntity.DATA_PER_SPAWN), seconds), 12, 80, GuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.spawner.nearby",
                menu.getData(EssenceSpawnerBlockEntity.DATA_NEARBY), menu.getData(EssenceSpawnerBlockEntity.DATA_CAP),
                menu.getData(EssenceSpawnerBlockEntity.DATA_RADIUS)), 12, 90, GuiStyle.TEXT_DIM, false);
    }
}
