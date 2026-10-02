package com.xtri6.grimsoul.menu;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.blockentity.OutputCrateBlockEntity;

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

/** Output Crate menu: 54 crate slots, then the player inventory. Works like a big chest. */
public class CrateMenu extends AbstractContainerMenu {
    public static final int WIDTH = 200;
    public static final int INV_Y = 160;
    private static final int X0 = (WIDTH - 162) / 2;
    public static final int PLAYER_START = OutputCrateBlockEntity.SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public CrateMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(OutputCrateBlockEntity.SLOTS), new SimpleContainerData(OutputCrateBlockEntity.DATA_COUNT));
    }

    public CrateMenu(int id, Inventory inv, OutputCrateBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.getItems(), be.getDataAccess());
    }

    private CrateMenu(int id, Inventory inv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(Registration.CRATE_MENU.get(), id);
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inv.player.level(), pos);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(items, row * 9 + col, X0 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, X0 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, X0 + col * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    public int getData(int index) {
        return data.get(index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide && player.level().getBlockEntity(pos) instanceof OutputCrateBlockEntity be) {
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
        } else if (!moveItemStackTo(stack, 0, PLAYER_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Registration.OUTPUT_CRATE.get());
    }
}
