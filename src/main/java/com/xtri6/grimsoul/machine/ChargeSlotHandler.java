package com.xtri6.grimsoul.machine;

import java.util.function.IntUnaryOperator;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A single slot that charges any Forge Energy item put in it (tools, batteries, jetpacks, armor
 * from other mods). Used by the Energy Cell and the Reaper Generator.
 */
public class ChargeSlotHandler extends ItemStackHandler {
    private final Runnable onChanged;

    public ChargeSlotHandler(Runnable onChanged) {
        super(1);
        this.onChanged = onChanged;
    }

    /** True if the item holds Forge Energy and can take more in. */
    public static boolean isChargeable(ItemStack stack) {
        IEnergyStorage storage = energyOf(stack);
        return storage != null && storage.canReceive();
    }

    @Nullable
    private static IEnergyStorage energyOf(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    public ItemStack getItem() {
        return getStackInSlot(0);
    }

    /** 0 to 1000: how full the item in the slot is, for the GUI. -1 when the slot is empty. */
    public int chargePermille() {
        IEnergyStorage storage = energyOf(getItem());
        if (storage == null || storage.getMaxEnergyStored() <= 0) {
            return -1;
        }
        return (int) ((long) storage.getEnergyStored() * 1000 / storage.getMaxEnergyStored());
    }

    /**
     * Moves up to {@code maxRate} FE into the item. {@code take} is asked for energy and returns how
     * much it actually gave (so the machine's own buffer and stats stay correct).
     * Returns the FE delivered.
     */
    public int charge(int maxRate, int available, IntUnaryOperator take) {
        IEnergyStorage storage = energyOf(getItem());
        if (storage == null || !storage.canReceive() || available <= 0 || maxRate <= 0) {
            return 0;
        }
        int wanted = storage.receiveEnergy(Math.min(maxRate, available), true);
        if (wanted <= 0) {
            return 0;
        }
        int given = take.applyAsInt(wanted);
        if (given <= 0) {
            return 0;
        }
        int accepted = storage.receiveEnergy(given, false);
        onChanged.run();
        return accepted;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isChargeable(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Charge", serializeNBT(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Charge")) {
            deserializeNBT(registries, tag.getCompound("Charge"));
        }
    }

    public void drop(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), getItem().copy());
        setStackInSlot(0, ItemStack.EMPTY);
    }
}
