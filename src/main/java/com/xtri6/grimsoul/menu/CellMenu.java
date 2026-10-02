package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.EnergyCellBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Energy Cell menu: no item slots, just the charge readout and side settings. */
public class CellMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public CellMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new SimpleContainerData(EnergyCellBlockEntity.DATA_COUNT));
    }

    public CellMenu(int id, Inventory inv, EnergyCellBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getDataAccess());
    }

    private CellMenu(int id, Inventory inv, BlockPos pos, ContainerData data) {
        super(Registration.CELL_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Registration.ENERGY_CELL.get());
    }
}
