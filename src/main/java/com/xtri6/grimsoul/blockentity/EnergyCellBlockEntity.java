package com.xtri6.grimsoul.blockentity;

import java.util.Arrays;

import javax.annotation.Nullable;

import com.xtri6.grimsoul.Config;
import com.xtri6.grimsoul.Registration;
import com.xtri6.grimsoul.block.EnergyCellBlock;
import com.xtri6.grimsoul.block.MachineBlock;
import com.xtri6.grimsoul.machine.RelativeSide;
import com.xtri6.grimsoul.menu.CellMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Reaper Energy Cell: stores FE. Each side is set in the GUI to take power in, push power out, or
 * do nothing. Keeps its charge when broken (shown on the item's tooltip).
 */
public class EnergyCellBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MODE_IN = 0, MODE_OUT = 1, MODE_OFF = 2;
    private static final int SIDES = RelativeSide.values().length;
    private static final int RATE_WINDOW = 20;

    public static final int BTN_SIDE = 0;
    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAPACITY_LO = 2, DATA_CAPACITY_HI = 3,
            DATA_IN_LO = 4, DATA_IN_HI = 5, DATA_OUT_LO = 6, DATA_OUT_HI = 7, DATA_SIDES = 8, DATA_COUNT = 9;

    private final CellEnergy energy = new CellEnergy(Config.CELL_CAPACITY.get(), Config.CELL_TRANSFER.get());
    private final int[] sideModes = new int[SIDES];

    // Average FE per tick in and out, measured over one second.
    private long inThisWindow, outThisWindow;
    private int inRate, outRate;
    private int windowTimer;

    private final IEnergyStorage inputOnly = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = energy.receiveEnergy(toReceive, simulate);
            if (!simulate) {
                inThisWindow += received;
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
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
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private final IEnergyStorage outputOnly = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = energy.extractEnergy(toExtract, simulate);
            if (!simulate) {
                outThisWindow += extracted;
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
                case DATA_IN_LO -> inRate & 0xFFFF;
                case DATA_IN_HI -> inRate >>> 16;
                case DATA_OUT_LO -> outRate & 0xFFFF;
                case DATA_OUT_HI -> outRate >>> 16;
                case DATA_SIDES -> packSides();
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

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.ENERGY_CELL_ENTITY.get(), pos, state);
        // Default: power comes in everywhere and goes out the front.
        Arrays.fill(sideModes, MODE_IN);
        sideModes[RelativeSide.FRONT.ordinal()] = MODE_OUT;
    }

    private int packSides() {
        int packed = 0;
        for (int i = 0; i < SIDES; i++) {
            packed |= sideModes[i] << (i * 2);
        }
        return packed;
    }

    public static int unpackSide(int packed, RelativeSide side) {
        return Math.min(MODE_OFF, (packed >> (side.ordinal() * 2)) & 3);
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCellBlockEntity be) {
        Direction facing = state.getValue(MachineBlock.FACING);
        int perSide = Config.CELL_TRANSFER.get();
        for (RelativeSide side : RelativeSide.values()) {
            if (be.sideModes[side.ordinal()] != MODE_OUT || be.energy.getEnergyStored() <= 0) {
                continue;
            }
            Direction dir = side.toDirection(facing);
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offer = Math.min(perSide, be.energy.getEnergyStored());
            int accepted = target.receiveEnergy(offer, false);
            if (accepted > 0) {
                be.energy.extractEnergy(accepted, false);
                be.outThisWindow += accepted;
                be.setChanged();
            }
        }
        if (++be.windowTimer >= RATE_WINDOW) {
            be.windowTimer = 0;
            be.inRate = (int) Math.min(Integer.MAX_VALUE, be.inThisWindow / RATE_WINDOW);
            be.outRate = (int) Math.min(Integer.MAX_VALUE, be.outThisWindow / RATE_WINDOW);
            be.inThisWindow = 0;
            be.outThisWindow = 0;
            // Charge gauge on the block's sides (0-8).
            int fill = be.energy.getEnergyStored() <= 0 ? 0
                    : 1 + (int) ((long) be.energy.getEnergyStored() * 7 / Math.max(1, be.energy.getMaxEnergyStored()));
            if (state.getValue(EnergyCellBlock.LEVEL) != fill) {
                level.setBlock(pos, state.setValue(EnergyCellBlock.LEVEL, fill), Block.UPDATE_CLIENTS);
            }
            MachineBlock.setActive(level, pos, level.getBlockState(pos), be.inRate > 0 || be.outRate > 0);
        }
    }

    // ---- Capabilities ----

    @Nullable
    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        if (side == null) {
            return energy;
        }
        RelativeSide relative = RelativeSide.fromDirection(getBlockState().getValue(MachineBlock.FACING), side);
        return switch (sideModes[relative.ordinal()]) {
            case MODE_IN -> inputOnly;
            case MODE_OUT -> outputOnly;
            default -> null;
        };
    }

    // ---- GUI ----

    public void handleButton(int id) {
        if (id >= BTN_SIDE && id < BTN_SIDE + SIDES) {
            int i = id - BTN_SIDE;
            sideModes[i] = (sideModes[i] + 1) % 3;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public int getEnergy() {
        return energy.getEnergyStored();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.grimsoul.energy_cell");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CellMenu(containerId, playerInventory, this);
    }

    // ---- Saving, and keeping the charge on the item ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Sides", packSides());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.set(tag.getInt("Energy"));
        if (tag.contains("Sides")) {
            int packed = tag.getInt("Sides");
            for (RelativeSide side : RelativeSide.values()) {
                sideModes[side.ordinal()] = unpackSide(packed, side);
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.getEnergyStored() > 0) {
            components.set(Registration.STORED_ENERGY.get(), energy.getEnergyStored());
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        energy.set(input.getOrDefault(Registration.STORED_ENERGY.get(), 0));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Energy");
    }

    private class CellEnergy extends EnergyStorage {
        CellEnergy(int capacity, int transfer) {
            super(capacity, transfer, Integer.MAX_VALUE);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }
}
