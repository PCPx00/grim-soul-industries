package com.xtri6.grimsoul.blockentity;

import java.util.Arrays;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.generator.GeneratorFuel;
import com.xtri6.grimsoul.machine.ItemSideMode;
import com.xtri6.grimsoul.machine.MachineControl;
import com.xtri6.grimsoul.machine.RedstoneMode;
import com.xtri6.grimsoul.machine.RelativeSide;
import com.xtri6.grimsoul.menu.GeneratorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Reaper Generator: burns mob drops into FE. Each side can be set (in the GUI) to take items in,
 * send items out, both, or nothing, and to push FE out or not. Items that aren't fuel can pass
 * straight through to the output slots, so a whole farm's drops can be piped in and the rest
 * flows on to storage.
 */
public class ReaperGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FUEL_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 9;
    private static final int EJECT_INTERVAL = 10;
    private static final int SIDES = RelativeSide.values().length;

    // Buttons: 0-5 cycle item mode per side, 6-11 toggle energy per side, then the switches.
    public static final int BTN_ITEM_SIDE = 0, BTN_ENERGY_SIDE = 6, BTN_POWER = 12, BTN_REDSTONE = 13,
            BTN_EJECT = 14, BTN_PASS = 15;
    // GUI data. Ints above 32767 are sent as two halves (menus sync data as shorts).
    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAPACITY_LO = 2, DATA_CAPACITY_HI = 3,
            DATA_RATE_LO = 4, DATA_RATE_HI = 5, DATA_BURN = 6, DATA_BURN_TOTAL = 7, DATA_ITEM_SIDES = 8,
            DATA_ENERGY_SIDES = 9, DATA_ENABLED = 10, DATA_REDSTONE = 11, DATA_EJECT = 12, DATA_PASS = 13,
            DATA_COUNT = 14;

    private final ItemStackHandler fuel = new ItemStackHandler(FUEL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return GeneratorFuel.of(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final GeneratorEnergy energy = new GeneratorEnergy(Config.GENERATOR_CAPACITY.get());
    private final MachineControl control = new MachineControl(RedstoneMode.IGNORE);

    private final ItemSideMode[] itemSides = new ItemSideMode[SIDES];
    private final boolean[] energySides = new boolean[SIDES];
    private boolean autoEject = true;
    private boolean passThrough = true;

    private int burnTime = 0;
    private int burnTotal = 0;
    private int burnRate = 0;
    private int lastRate = 0;
    private int ejectTimer = 0;

    private final IEnergyStorage outputOnlyEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = energy.extractEnergy(Math.min(toExtract, Config.GENERATOR_MAX_OUTPUT.get()), simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_LO -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HI -> energy.getEnergyStored() >>> 16;
                case DATA_CAPACITY_LO -> energy.getMaxEnergyStored() & 0xFFFF;
                case DATA_CAPACITY_HI -> energy.getMaxEnergyStored() >>> 16;
                case DATA_RATE_LO -> lastRate & 0xFFFF;
                case DATA_RATE_HI -> lastRate >>> 16;
                case DATA_BURN -> Math.min(burnTime, 0x7FFF);
                case DATA_BURN_TOTAL -> Math.min(burnTotal, 0x7FFF);
                case DATA_ITEM_SIDES -> packItemSides();
                case DATA_ENERGY_SIDES -> packEnergySides();
                case DATA_ENABLED -> control.isEnabled() ? 1 : 0;
                case DATA_REDSTONE -> control.getRedstoneMode().ordinal();
                case DATA_EJECT -> autoEject ? 1 : 0;
                case DATA_PASS -> passThrough ? 1 : 0;
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

    public ReaperGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.REAPER_GENERATOR_ENTITY.get(), pos, state);
        // Defaults: every side takes items in and sends items out, FE goes out every side but the front.
        Arrays.fill(itemSides, ItemSideMode.BOTH);
        Arrays.fill(energySides, true);
        energySides[RelativeSide.FRONT.ordinal()] = false;
    }

    /** Rebuilds the side settings from the packed GUI values (2 bits per side for items, 1 bit for energy). */
    public static ItemSideMode unpackItemSide(int packed, RelativeSide side) {
        return ItemSideMode.byIndex((packed >> (side.ordinal() * 2)) & 3);
    }

    public static boolean unpackEnergySide(int packed, RelativeSide side) {
        return ((packed >> side.ordinal()) & 1) != 0;
    }

    private int packItemSides() {
        int packed = 0;
        for (int i = 0; i < SIDES; i++) {
            packed |= itemSides[i].ordinal() << (i * 2);
        }
        return packed;
    }

    private int packEnergySides() {
        int packed = 0;
        for (int i = 0; i < SIDES; i++) {
            if (energySides[i]) {
                packed |= 1 << i;
            }
        }
        return packed;
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReaperGeneratorBlockEntity be) {
        boolean enabled = be.control.isActive(level, pos);
        boolean burning = enabled && be.burn();
        MachineBlock.setActive(level, pos, state, burning);
        be.pushEnergy(level, pos, state);
        if (be.autoEject && ++be.ejectTimer >= EJECT_INTERVAL) {
            be.ejectTimer = 0;
            be.ejectItems(level, pos, state);
        }
    }

    /** Burns fuel for one tick. Pauses (without wasting fuel) while the energy buffer is full. */
    private boolean burn() {
        int space = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (burnTime <= 0) {
            lastRate = 0;
            if (space <= 0 || !startNextFuel()) {
                return false;
            }
        }
        if (space < burnRate) {
            // Full: hold the current fuel until there's room for a full tick of power.
            lastRate = 0;
            return false;
        }
        energy.generate(burnRate);
        lastRate = burnRate;
        burnTime--;
        setChanged();
        return true;
    }

    private boolean startNextFuel() {
        for (int i = 0; i < fuel.getSlots(); i++) {
            ItemStack stack = fuel.getStackInSlot(i);
            GeneratorFuel value = GeneratorFuel.of(stack);
            if (value != null) {
                fuel.extractItem(i, 1, false);
                burnRate = (int) Math.min(Integer.MAX_VALUE, Math.round(value.fePerTick() * Config.GENERATOR_FUEL_MULTIPLIER.get()));
                burnTime = value.burnTime();
                burnTotal = value.burnTime();
                return burnRate > 0;
            }
        }
        return false;
    }

    private void pushEnergy(Level level, BlockPos pos, BlockState state) {
        if (energy.getEnergyStored() <= 0) {
            return;
        }
        Direction facing = state.getValue(MachineBlock.FACING);
        int perSide = Config.GENERATOR_MAX_OUTPUT.get();
        for (RelativeSide side : RelativeSide.values()) {
            if (!energySides[side.ordinal()]) {
                continue;
            }
            Direction dir = side.toDirection(facing);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offer = Math.min(perSide, energy.getEnergyStored());
            int accepted = target.receiveEnergy(offer, false);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
                setChanged();
            }
            if (energy.getEnergyStored() <= 0) {
                return;
            }
        }
    }

    /** Pushes pass-through items into chests, pipes or storage on every side set to send items out. */
    private void ejectItems(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(MachineBlock.FACING);
        for (RelativeSide side : RelativeSide.values()) {
            if (!itemSides[side.ordinal()].canOutput()) {
                continue;
            }
            Direction dir = side.toDirection(facing);
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null) {
                continue;
            }
            for (int i = 0; i < output.getSlots(); i++) {
                ItemStack stack = output.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                if (left.getCount() != stack.getCount()) {
                    output.setStackInSlot(i, left);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Capabilities
    // ------------------------------------------------------------------------------------------

    /** FE for cables, Pipez and other mods: only on sides set to send FE out. */
    @Nullable
    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        if (side == null) {
            return outputOnlyEnergy;
        }
        RelativeSide relative = RelativeSide.fromDirection(getBlockState().getValue(MachineBlock.FACING), side);
        return energySides[relative.ordinal()] ? outputOnlyEnergy : null;
    }

    /** Items for pipes, hoppers, AE2 and storage mods, following that side's item setting. */
    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        ItemSideMode mode = ItemSideMode.BOTH;
        if (side != null) {
            mode = itemSides[RelativeSide.fromDirection(getBlockState().getValue(MachineBlock.FACING), side).ordinal()];
        }
        return mode == ItemSideMode.OFF ? null : new SideItemHandler(mode);
    }

    /**
     * Slots 0-8 are fuel (insert only, and only fuel), slots 9-17 are the pass-through output
     * (extract only; non-fuel items go in here when pass-through is on).
     */
    private class SideItemHandler implements IItemHandler {
        private final ItemSideMode mode;

        SideItemHandler(ItemSideMode mode) {
            this.mode = mode;
        }

        @Override
        public int getSlots() {
            return FUEL_SLOTS + OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < FUEL_SLOTS ? fuel.getStackInSlot(slot) : output.getStackInSlot(slot - FUEL_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!mode.canInput() || stack.isEmpty()) {
                return stack;
            }
            boolean isFuel = GeneratorFuel.of(stack) != null;
            if (slot < FUEL_SLOTS) {
                return isFuel ? fuel.insertItem(slot, stack, simulate) : stack;
            }
            if (isFuel || !passThrough) {
                return stack;
            }
            return output.insertItem(slot - FUEL_SLOTS, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!mode.canOutput() || slot < FUEL_SLOTS) {
                return ItemStack.EMPTY;
            }
            return output.extractItem(slot - FUEL_SLOTS, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            boolean isFuel = GeneratorFuel.of(stack) != null;
            return slot < FUEL_SLOTS ? isFuel : passThrough && !isFuel;
        }
    }

    // ------------------------------------------------------------------------------------------
    // GUI
    // ------------------------------------------------------------------------------------------

    public void handleButton(int id, Player player) {
        if (id >= BTN_ITEM_SIDE && id < BTN_ITEM_SIDE + SIDES) {
            int i = id - BTN_ITEM_SIDE;
            itemSides[i] = itemSides[i].next();
        } else if (id >= BTN_ENERGY_SIDE && id < BTN_ENERGY_SIDE + SIDES) {
            int i = id - BTN_ENERGY_SIDE;
            energySides[i] = !energySides[i];
        } else {
            switch (id) {
                case BTN_POWER -> control.togglePower();
                case BTN_REDSTONE -> control.cycleRedstone();
                case BTN_EJECT -> autoEject = !autoEject;
                case BTN_PASS -> passThrough = !passThrough;
                default -> {
                    return;
                }
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            // Pipes and cables re-check which sides connect.
            level.invalidateCapabilities(worldPosition);
        }
    }

    public ItemStackHandler getFuel() {
        return fuel;
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (ItemStackHandler handler : new ItemStackHandler[] {fuel, output}) {
            for (int i = 0; i < handler.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i).copy());
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.reaper_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GeneratorMenu(containerId, playerInventory, this);
    }

    // ------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel", fuel.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTotal", burnTotal);
        tag.putInt("BurnRate", burnRate);
        tag.putInt("ItemSides", packItemSides());
        tag.putInt("EnergySides", packEnergySides());
        tag.putBoolean("AutoEject", autoEject);
        tag.putBoolean("PassThrough", passThrough);
        control.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fuel")) {
            fuel.deserializeNBT(registries, tag.getCompound("Fuel"));
        }
        if (tag.contains("Output")) {
            output.deserializeNBT(registries, tag.getCompound("Output"));
        }
        energy.setEnergy(tag.getInt("Energy"));
        burnTime = tag.getInt("BurnTime");
        burnTotal = tag.getInt("BurnTotal");
        burnRate = tag.getInt("BurnRate");
        if (tag.contains("ItemSides")) {
            int packed = tag.getInt("ItemSides");
            for (RelativeSide side : RelativeSide.values()) {
                itemSides[side.ordinal()] = unpackItemSide(packed, side);
            }
        }
        if (tag.contains("EnergySides")) {
            int packed = tag.getInt("EnergySides");
            for (RelativeSide side : RelativeSide.values()) {
                energySides[side.ordinal()] = unpackEnergySide(packed, side);
            }
        }
        if (tag.contains("AutoEject")) {
            autoEject = tag.getBoolean("AutoEject");
        }
        if (tag.contains("PassThrough")) {
            passThrough = tag.getBoolean("PassThrough");
        }
        control.load(tag);
    }

    /** The FE buffer. Only the generator itself can add power; cables can only take it out. */
    private static class GeneratorEnergy extends EnergyStorage {
        GeneratorEnergy(int capacity) {
            super(capacity, 0, Integer.MAX_VALUE);
        }

        void generate(int amount) {
            energy = Math.min(capacity, energy + amount);
        }

        void setEnergy(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }
}
