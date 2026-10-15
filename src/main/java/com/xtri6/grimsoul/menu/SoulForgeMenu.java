package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.SoulForgeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Soul Forge menu. Slot 0: the item to forge. Slot 1: Soul Steel. Slot 2: the result (take only).
 * Slots 3-38: player inventory and hotbar.
 */
public class SoulForgeMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 166;
    public static final int WORK_X = 44, WORK_Y = 36, STEEL_X = 44, STEEL_Y = 56, OUTPUT_X = 116, OUTPUT_Y = 46;
    public static final int INV_X = 8, INV_Y = 84, HOTBAR_Y = 142;
    public static final int PLAYER_START = 3, PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public SoulForgeMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(3), new SimpleContainerData(SoulForgeBlockEntity.DATA_COUNT));
    }

    public SoulForgeMenu(int id, Inventory inv, SoulForgeBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getItems(), be.getDataAccess());
    }

    private SoulForgeMenu(int id, Inventory inv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(Registration.SOUL_FORGE_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);

        addSlot(new SlotItemHandler(items, SoulForgeBlockEntity.SLOT_WORK, WORK_X, WORK_Y));
        addSlot(new SlotItemHandler(items, SoulForgeBlockEntity.SLOT_STEEL, STEEL_X, STEEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Registration.SOUL_STEEL.get());
            }
        });
        addSlot(new SlotItemHandler(items, SoulForgeBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
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
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == SoulForgeBlockEntity.SLOT_OUTPUT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SoulForgeBlockEntity.SLOT_WORK, SoulForgeBlockEntity.SLOT_OUTPUT, false)) {
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
        return stillValid(access, player, Registration.SOUL_FORGE.get());
    }
}
