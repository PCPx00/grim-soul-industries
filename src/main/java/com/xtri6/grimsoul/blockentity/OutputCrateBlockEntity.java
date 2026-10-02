package com.xtri6.grimsoul.blockentity;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.menu.CrateMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Reaper Output Crate: 54 slots of storage for Simulation Chambers (or anything else). Pipes,
 * hoppers and AE2 can put items in and take them out from any side. With Void Overflow on, items
 * that don't fit are deleted so the machines feeding it never stall.
 */
public class OutputCrateBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 54;
    public static final int BTN_VOID = 0;
    public static final int DATA_VOID = 0, DATA_USED = 1, DATA_COUNT = 2;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean voidOverflow = false;

    /**
     * What pipes see: the 54 real slots, plus one extra slot at the end that swallows anything when
     * Void Overflow is on. Pipes and hoppers fill slots in order, so the void slot is only reached
     * once the crate is full.
     */
    private final IItemHandler pipeHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < SLOTS ? items.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < SLOTS) {
                return items.insertItem(slot, stack, simulate);
            }
            return voidOverflow ? ItemStack.EMPTY : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < SLOTS ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < SLOTS || voidOverflow;
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_VOID -> voidOverflow ? 1 : 0;
                case DATA_USED -> usedSlots();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OutputCrateBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.OUTPUT_CRATE_ENTITY.get(), pos, state);
    }

    private int usedSlots() {
        int used = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    public void handleButton(int id) {
        if (id == BTN_VOID) {
            voidOverflow = !voidOverflow;
            setChanged();
        }
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public IItemHandler getPipeHandler() {
        return pipeHandler;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOTS; i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i).copy());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.output_crate");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrateMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putBoolean("VoidOverflow", voidOverflow);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
        voidOverflow = tag.getBoolean("VoidOverflow");
    }
}
