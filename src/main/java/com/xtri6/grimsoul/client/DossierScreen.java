package com.xtri6.grimsoul.client;

import java.util.List;
import java.util.Locale;

import com.xtri6.grimsoul.blockentity.SimulationChamberBlockEntity;
import com.xtri6.grimsoul.item.DataCoreItem;
import com.xtri6.grimsoul.menu.SimulationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Loot Dossier: the chamber's sampled average drops per run for the bound mob. */
public class DossierScreen extends Screen {
    private static final int W = 220, H = 218, ROWS = 9, ROW_H = 16;

    private final SimulationScreen parent;
    private final BlockPos pos;
    /** The menu's core slot is synced to the client; the block entity's core copy is not. */
    private final SimulationMenu menu;

    public DossierScreen(SimulationScreen parent, BlockPos pos, SimulationMenu menu) {
        super(Component.translatable("gui.grimsoul.dossier.title"));
        this.parent = parent;
        this.pos = pos;
        this.menu = menu;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.grimsoul.dossier.sample"),
                        b -> parent.press(SimulationChamberBlockEntity.BTN_DOSSIER))
                .bounds(x + 12, y + H - 26, 96, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.grimsoul.dossier.back"),
                        b -> minecraft.setScreen(parent))
                .bounds(x + W - 108, y + H - 26, 96, 16).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = (width - W) / 2;
        int y = (height - H) / 2;
        GuiStyle.panel(graphics, x, y, x + W, y + H);
    }

    private SimulationChamberBlockEntity chamber() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        BlockEntity be = minecraft.level.getBlockEntity(pos);
        return be instanceof SimulationChamberBlockEntity chamber ? chamber : null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = (width - W) / 2;
        int y = (height - H) / 2;
        graphics.drawString(font, title, x + 12, y + 10, GuiStyle.TEXT_ACCENT, false);

        SimulationChamberBlockEntity be = chamber();
        if (be == null) {
            return;
        }
        CompoundTag dossier = be.getDossier();
        String current = DataCoreItem.getMob(menu.slots.get(0).getItem())
                .map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()).orElse("");
        if (dossier.isEmpty() || !current.equals(dossier.getString("Mob"))) {
            Component message = current.isEmpty() ? Component.translatable("gui.grimsoul.dossier.empty")
                    : Component.translatable("gui.grimsoul.dossier.sampling");
            graphics.drawWordWrap(font, message, x + 12, y + 40, W - 24, GuiStyle.TEXT_DIM);
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(current));
        graphics.drawString(font, type.getDescription(), x + 12, y + 28, GuiStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.grimsoul.dossier.runs",
                dossier.getInt("Samples"), dossier.getInt("Chance")), x + 12, y + 40, GuiStyle.TEXT_DIM, false);
        if (dossier.getBoolean("PlayerKill") && dossier.getInt("Xp") > 0) {
            graphics.drawString(font, Component.translatable("gui.grimsoul.dossier.xp", dossier.getInt("Xp")),
                    x + 12, y + 52, GuiStyle.TEXT_ENERGY, false);
        }
        Component setup = Component.translatable("gui.grimsoul.dossier.setup", dossier.getInt("Looting"),
                Component.translatable(dossier.getBoolean("PlayerKill") ? "gui.grimsoul.dossier.on" : "gui.grimsoul.dossier.off"));
        graphics.drawString(font, setup, x + 12, y + 64, GuiStyle.TEXT_DIM, false);

        ListTag items = dossier.getList("Items", Tag.TAG_COMPOUND);
        int top = y + 80;
        for (int i = 0; i < Math.min(items.size(), ROWS); i++) {
            CompoundTag entry = items.getCompound(i);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("Id")));
            int rowY = top + i * ROW_H;
            ItemStack stack = new ItemStack(item);
            graphics.renderItem(stack, x + 12, rowY);
            graphics.drawString(font, stack.getHoverName(), x + 34, rowY + 4, GuiStyle.TEXT, false);
            String avg = String.format(Locale.ROOT, "%.2f", entry.getDouble("Avg"));
            graphics.drawString(font, Component.translatable("gui.grimsoul.dossier.per_run", avg),
                    x + W - 12 - font.width(Component.translatable("gui.grimsoul.dossier.per_run", avg)), rowY + 4, GuiStyle.TEXT_OK, false);
        }
        if (items.size() > ROWS) {
            graphics.drawString(font, Component.translatable("gui.grimsoul.dossier.more", items.size() - ROWS),
                    x + 12, top + ROWS * ROW_H + 2, GuiStyle.TEXT_DIM, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
