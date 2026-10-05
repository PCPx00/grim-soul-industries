package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.EnergyCellBlockEntity;
import com.xtri6.grimsoul.machine.ChargeSlotHandler;

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

/**
 * Energy Cell menu: the charge readout, side settings, one charge slot (slot 0) and the player
 * inventory (slots 1-36).
 */
public class CellMenu extends AbstractContainerMenu {
    public static final int WIDTH = 232;
    public static final int CHARGE_X = 42, CHARGE_Y = 80;
    public static final int INV_Y = 132;
    private static final int INV_X = (WIDTH - 162) / 2;
    public static final int PLAYER_START = 1;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public CellMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ChargeSlotHandler(() -> {}), new SimpleContainerData(EnergyCellBlockEntity.DATA_COUNT));
    }

    public CellMenu(int id, Inventory inv, EnergyCellBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getChargeSlot(), be.getDataAccess());
    }

    private CellMenu(int id, Inventory inv, BlockPos pos, IItemHandler charge, ContainerData data) {
        super(Registration.CELL_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        addSlot(new ChargeSlot(charge, CHARGE_X, CHARGE_Y));
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

    public int getSplit(int lowIndex) {
        return (data.get(lowIndex) & 0xFFFF) | ((data.get(lowIndex + 1) & 0xFFFF) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof EnergyCellBlockEntity be) {
            be.handleButton(id);
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
        } else if (!ChargeSlotHandler.isChargeable(stack) || !moveItemStackTo(stack, 0, PLAYER_START, false)) {
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
        return stillValid(access, player, Registration.ENERGY_CELL.get());
    }
}
