package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.EssenceSpawnerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Essence Spawner menu. Slot 0: Reaper's Lantern. Slots 1-6: one card slot per spawner card.
 * Slots 7-42: player inventory and hotbar.
 */
public class SpawnerMenu extends AbstractContainerMenu {
    public static final int WIDTH = 250;
    public static final int LANTERN_X = 12, LANTERN_Y = 22;
    public static final int CARDS_X = 12, CARDS_Y = 56, CARD_SPACING = 22;
    public static final int INV_Y = 146;
    private static final int INV_X = (WIDTH - 162) / 2;
    public static final int CARD_START = 1;
    public static final int PLAYER_START = CARD_START + EssenceSpawnerBlockEntity.CARD_COUNT;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public SpawnerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(1), EssenceSpawnerBlockEntity.createCardHandler(() -> {}),
                new SimpleContainerData(EssenceSpawnerBlockEntity.DATA_COUNT));
    }

    public SpawnerMenu(int id, Inventory inv, EssenceSpawnerBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getLantern(), be.getCards(), be.getDataAccess());
    }

    private SpawnerMenu(int id, Inventory inv, BlockPos pos, IItemHandler lantern, IItemHandler cards, ContainerData data) {
        super(Registration.SPAWNER_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        addSlot(new SlotItemHandler(lantern, 0, LANTERN_X, LANTERN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof com.xtri6.grimsoul.item.ReapersLanternItem;
            }
        });
        for (int i = 0; i < EssenceSpawnerBlockEntity.CARD_COUNT; i++) {
            addSlot(new CardSlot(cards, i, CARDS_X + i * CARD_SPACING, CARDS_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    public int getData(int index) {
        return data.get(index);
    }

    /** Reads an int the block entity split into two 16-bit halves. */
    public int getSplit(int lowIndex) {
        return (data.get(lowIndex) & 0xFFFF) | ((data.get(lowIndex + 1) & 0xFFFF) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof EssenceSpawnerBlockEntity be) {
            be.handleButton(id, player);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, PLAYER_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Registration.ESSENCE_SPAWNER.get());
    }
}
